package br.com.spolador.ecommerce.inventory_service.service.impl;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.exception.InsufficientStockException;
import br.com.spolador.ecommerce.inventory_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.inventory_service.exception.SkuAlreadyExistsException;
import br.com.spolador.ecommerce.inventory_service.mapper.InventoryMapper;
import br.com.spolador.ecommerce.inventory_service.model.Inventory;
import br.com.spolador.ecommerce.inventory_service.repository.InventoryRepository;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.model.ProcessedOrder;
import br.com.spolador.ecommerce.inventory_service.repository.ProcessedOrderRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProcessedOrderRepository processedOrderRepository;
    private final InventoryMapper inventoryMapper;
    @Value("${inventory.allow-backorders:false}")
    private boolean allowBackOrders;

    @Override
    @Transactional(readOnly = true)
    public boolean isInStock(String sku, Integer quantity) {
        if(allowBackOrders) {
            log.warn("Backorder active: authorizing stock for sku: {}", sku);
            return true;
        }
        return inventoryRepository.findBySku(sku)
                .map(inventory -> inventory.getQuantity() >= quantity)
                .orElse(false);
    }

    @Override
    @Transactional
    public InventoryResponseDTO createInventory(InventoryRequestDTO inventoryRequestDTO) {
        boolean exists = inventoryRepository.existsBySku(inventoryRequestDTO.getSku());
        if(exists) {
            throw new SkuAlreadyExistsException(inventoryRequestDTO.getSku());
        }
        Inventory inventory = inventoryMapper.toModel(inventoryRequestDTO);
        Inventory createdInventory = inventoryRepository.save(inventory);
        log.info("The inventory created with SKU: {}", createdInventory.getSku());
        return inventoryMapper.toResponse(createdInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponseDTO> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public InventoryResponseDTO updateInventoryById(Long id, InventoryRequestDTO inventoryRequestDTO) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Inventory", "id", id)
                );
        inventory.setSku(inventoryRequestDTO.getSku());
        inventory.setQuantity(inventoryRequestDTO.getQuantity());
        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Inventory updated for id: {}", id);
        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public void deleteInventoryById(Long id) {
        if(!inventoryRepository.existsById(id)){
            throw new ResourceNotFoundException("Inventory", "id", id);
        }
        inventoryRepository.deleteById(id);
        log.info("The inventory with id {} was deleted", id);
    }

    @Override
    @Transactional
    public void reduceStock(String sku, Integer quantity) {
        var inventory = inventoryRepository.findBySkuWithLock(sku)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Inventory", "sku", sku)
                );
        if(inventory.getQuantity() < quantity) {
            throw new InsufficientStockException(sku, quantity, inventory.getQuantity());
        }
        inventory.setQuantity(inventory.getQuantity()-quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processOrderStockReduction(String orderNumber, List<OrderCreatedEvent.OrderItemEvent> items) {
        if (processedOrderRepository.existsByOrderNumber(orderNumber)) {
            log.warn("Order {} was already processed. Skipping stock reduction to maintain idempotency.", orderNumber);
            return false;
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty");
        }

        // Sort items by SKU alphabetically to prevent database deadlocks under concurrent orders
        List<OrderCreatedEvent.OrderItemEvent> sortedItems = items.stream()
                .sorted(Comparator.comparing(OrderCreatedEvent.OrderItemEvent::sku))
                .toList();

        if (allowBackOrders) {
            log.warn("Backorder active: authorizing stock reduction bypass for order: {}", orderNumber);
        } else {
            for (OrderCreatedEvent.OrderItemEvent item : sortedItems) {
                Inventory inventory = inventoryRepository.findBySkuWithLock(item.sku())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory", "sku", item.sku()));

                if (inventory.getQuantity() < item.quantity()) {
                    throw new InsufficientStockException(item.sku(), item.quantity(), inventory.getQuantity());
                }

                inventory.setQuantity(inventory.getQuantity() - item.quantity());
                inventoryRepository.save(inventory);
            }
        }

        processedOrderRepository.save(ProcessedOrder.builder()
                .orderNumber(orderNumber)
                .processedAt(LocalDateTime.now())
                .build());

        return true;
    }
}
