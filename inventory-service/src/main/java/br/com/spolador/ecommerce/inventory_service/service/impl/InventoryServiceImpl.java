package br.com.spolador.ecommerce.inventory_service.service.impl;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.inventory_service.mapper.InventoryMapper;
import br.com.spolador.ecommerce.inventory_service.model.Inventory;
import br.com.spolador.ecommerce.inventory_service.repository.InventoryRepository;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean isInStock(String sku, Integer quantity) {
        return inventoryRepository.findBySku(sku)
                .map(inventory -> inventory.getQuantity() >= quantity)
                .orElse(false);
    }

    @Override
    @Transactional
    public InventoryResponseDTO createInventory(InventoryRequestDTO inventoryRequestDTO) {
        boolean exists = inventoryRepository.existsBySku(inventoryRequestDTO.getSku());
        if(exists) {
            throw new RuntimeException("The inventory for SKU " + inventoryRequestDTO.getSku() + " already exists");
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
        inventory.setQuantity(inventory.getQuantity());
        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Inventory updated for id: {}", id);
        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public void deleteInventoryById(Long id) {
        if(!inventoryRepository.existsById(id)){
            throw new ResourceNotFoundException("inventory", "id", id);
        }
        inventoryRepository.deleteById(id);
        log.info("The inventory with id {} was deleted", id);
    }

    @Override
    @Transactional
    public void reduceStock(String sku, Integer quantity) {
        var inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(
                        () -> new RuntimeException("Product not found for this sku: " + sku)
                );
        if(inventory.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for: " + sku);
        }
        inventory.setQuantity(inventory.getQuantity()-quantity);
        inventoryRepository.save(inventory);
    }
}
