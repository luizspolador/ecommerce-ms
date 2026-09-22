package br.com.spolador.ecommerce.inventory_service.service.impl;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.exception.InsufficientStockException;
import br.com.spolador.ecommerce.inventory_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.inventory_service.exception.SkuAlreadyExistsException;
import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import br.com.spolador.ecommerce.inventory_service.mapper.InventoryMapper;
import br.com.spolador.ecommerce.inventory_service.model.Inventory;
import br.com.spolador.ecommerce.inventory_service.repository.InventoryRepository;
import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.model.ProcessedOrder;
import br.com.spolador.ecommerce.inventory_service.repository.ProcessedOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for InventoryServiceImpl")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedOrderRepository processedOrderRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Nested
    @DisplayName("isInStock()")
    class IsInStockTests {

        @Test
        @DisplayName("When backorders allowed, should always return true")
        void whenAllowBackordersTrue_shouldReturnTrue() {
            ReflectionTestUtils.setField(inventoryService, "allowBackOrders", true);

            boolean inStock = inventoryService.isInStock("ANY_SKU", 100);

            assertThat(inStock).isTrue();
            verifyNoInteractions(inventoryRepository);
        }

        @Test
        @DisplayName("When backorders not allowed and sku found with sufficient quantity, should return true")
        void whenSkuFoundWithSufficientStock_shouldReturnTrue() {
            ReflectionTestUtils.setField(inventoryService, "allowBackOrders", false);
            Inventory inventory = InventoryFactory.createInventory(); // quantity = 50
            when(inventoryRepository.findBySku(InventoryFactory.DEFAULT_SKU)).thenReturn(Optional.of(inventory));

            boolean inStock = inventoryService.isInStock(InventoryFactory.DEFAULT_SKU, 10);

            assertThat(inStock).isTrue();
            verify(inventoryRepository).findBySku(InventoryFactory.DEFAULT_SKU);
        }

        @Test
        @DisplayName("When backorders not allowed and sku found with insufficient quantity, should return false")
        void whenSkuFoundWithInsufficientStock_shouldReturnFalse() {
            ReflectionTestUtils.setField(inventoryService, "allowBackOrders", false);
            Inventory inventory = InventoryFactory.createInventory(); // quantity = 50
            when(inventoryRepository.findBySku(InventoryFactory.DEFAULT_SKU)).thenReturn(Optional.of(inventory));

            boolean inStock = inventoryService.isInStock(InventoryFactory.DEFAULT_SKU, 100);

            assertThat(inStock).isFalse();
            verify(inventoryRepository).findBySku(InventoryFactory.DEFAULT_SKU);
        }

        @Test
        @DisplayName("When backorders not allowed and sku not found, should return false")
        void whenSkuNotFound_shouldReturnFalse() {
            ReflectionTestUtils.setField(inventoryService, "allowBackOrders", false);
            when(inventoryRepository.findBySku("UNKNOWN")).thenReturn(Optional.empty());

            boolean inStock = inventoryService.isInStock("UNKNOWN", 1);

            assertThat(inStock).isFalse();
            verify(inventoryRepository).findBySku("UNKNOWN");
        }
    }

    @Nested
    @DisplayName("createInventory()")
    class CreateInventoryTests {

        @Test
        @DisplayName("When sku does not exist, should create and return InventoryResponseDTO")
        void whenSkuDoesNotExist_shouldCreateInventory() {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();
            Inventory inventoryToSave = InventoryFactory.createInventoryWithoutId();
            Inventory savedInventory = InventoryFactory.createInventory();
            InventoryResponseDTO expectedResponse = InventoryFactory.createInventoryResponseDTO();

            when(inventoryRepository.existsBySku(requestDTO.getSku())).thenReturn(false);
            when(inventoryMapper.toModel(requestDTO)).thenReturn(inventoryToSave);
            when(inventoryRepository.save(inventoryToSave)).thenReturn(savedInventory);
            when(inventoryMapper.toResponse(savedInventory)).thenReturn(expectedResponse);

            InventoryResponseDTO actualResponse = inventoryService.createInventory(requestDTO);

            assertThat(actualResponse).isNotNull().isEqualTo(expectedResponse);
            verify(inventoryRepository).existsBySku(requestDTO.getSku());
            verify(inventoryRepository).save(inventoryToSave);
        }

        @Test
        @DisplayName("When sku already exists, should throw SkuAlreadyExistsException")
        void whenSkuAlreadyExists_shouldThrowSkuAlreadyExistsException() {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();
            when(inventoryRepository.existsBySku(requestDTO.getSku())).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.createInventory(requestDTO))
                    .isInstanceOf(SkuAlreadyExistsException.class)
                    .hasMessageContaining("already exists");

            verify(inventoryRepository).existsBySku(requestDTO.getSku());
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllInventories()")
    class GetAllInventoriesTests {

        @Test
        @DisplayName("Should return list of all inventory items as response DTOs")
        void shouldReturnAllInventories() {
            List<Inventory> list = InventoryFactory.createInventoryList();
            when(inventoryRepository.findAll()).thenReturn(list);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(InventoryFactory.createInventoryResponseDTO());

            List<InventoryResponseDTO> result = inventoryService.getAllInventories();

            assertThat(result).hasSize(3);
            verify(inventoryRepository).findAll();
            verify(inventoryMapper, times(3)).toResponse(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return empty list when no inventory records exist")
        void whenEmpty_shouldReturnEmptyList() {
            when(inventoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<InventoryResponseDTO> result = inventoryService.getAllInventories();

            assertThat(result).isEmpty();
            verify(inventoryRepository).findAll();
            verifyNoInteractions(inventoryMapper);
        }
    }

    @Nested
    @DisplayName("updateInventoryById()")
    class UpdateInventoryByIdTests {

        @Test
        @DisplayName("When inventory found, should update and return updated response DTO")
        void whenInventoryFound_shouldUpdate() {
            Long id = InventoryFactory.DEFAULT_ID;
            InventoryRequestDTO updateDTO = InventoryFactory.createCustomInventoryRequestDTO("SKU-UPDATED", 80);
            Inventory existing = InventoryFactory.createInventory();
            Inventory saved = InventoryFactory.createCustomInventory(id, "SKU-UPDATED", 80);
            InventoryResponseDTO expectedResponse = InventoryResponseDTO.builder()
                    .id(id).sku("SKU-UPDATED").quantity(80).inStock(true).build();

            when(inventoryRepository.findById(id)).thenReturn(Optional.of(existing));
            when(inventoryRepository.save(existing)).thenReturn(saved);
            when(inventoryMapper.toResponse(saved)).thenReturn(expectedResponse);

            InventoryResponseDTO actualResponse = inventoryService.updateInventoryById(id, updateDTO);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            assertThat(existing.getSku()).isEqualTo("SKU-UPDATED");
            assertThat(existing.getQuantity()).isEqualTo(80);
            verify(inventoryRepository).findById(id);
            verify(inventoryRepository).save(existing);
        }

        @Test
        @DisplayName("When inventory not found, should throw ResourceNotFoundException")
        void whenInventoryNotFound_shouldThrowResourceNotFoundException() {
            Long id = 999L;
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();
            when(inventoryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.updateInventoryById(id, requestDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory not found with id: '999'");

            verify(inventoryRepository).findById(id);
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteInventoryById()")
    class DeleteInventoryByIdTests {

        @Test
        @DisplayName("When inventory exists, should delete successfully")
        void whenInventoryExists_shouldDelete() {
            Long id = InventoryFactory.DEFAULT_ID;
            when(inventoryRepository.existsById(id)).thenReturn(true);

            inventoryService.deleteInventoryById(id);

            verify(inventoryRepository).existsById(id);
            verify(inventoryRepository).deleteById(id);
        }

        @Test
        @DisplayName("When inventory does not exist, should throw ResourceNotFoundException")
        void whenInventoryDoesNotExist_shouldThrowResourceNotFoundException() {
            Long id = 999L;
            when(inventoryRepository.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> inventoryService.deleteInventoryById(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory not found with id: '999'");

            verify(inventoryRepository).existsById(id);
            verify(inventoryRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("reduceStock()")
    class ReduceStockTests {

        @Test
        @DisplayName("When sku found and quantity available, should reduce quantity and save")
        void whenStockSufficient_shouldReduceStock() {
            String sku = InventoryFactory.DEFAULT_SKU;
            Inventory inventory = InventoryFactory.createInventory(); // quantity = 50
            when(inventoryRepository.findBySkuWithLock(sku)).thenReturn(Optional.of(inventory));

            inventoryService.reduceStock(sku, 10);

            assertThat(inventory.getQuantity()).isEqualTo(40);
            verify(inventoryRepository).findBySkuWithLock(sku);
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("When sku not found, should throw ResourceNotFoundException")
        void whenSkuNotFound_shouldThrowResourceNotFoundException() {
            when(inventoryRepository.findBySkuWithLock("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.reduceStock("UNKNOWN", 5))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Inventory not found with sku: 'UNKNOWN'");

            verify(inventoryRepository).findBySkuWithLock("UNKNOWN");
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("When stock is insufficient, should throw InsufficientStockException")
        void whenStockInsufficient_shouldThrowInsufficientStockException() {
            String sku = InventoryFactory.DEFAULT_SKU;
            Inventory inventory = InventoryFactory.createCustomInventory(1L, sku, 5);
            when(inventoryRepository.findBySkuWithLock(sku)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.reduceStock(sku, 10))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock for SKU 'IPHONE_15_BLACK'");

            verify(inventoryRepository).findBySkuWithLock(sku);
            verify(inventoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processOrderStockReduction()")
    class ProcessOrderStockReductionTests {

        @Test
        @DisplayName("When order was already processed, should return false and skip reduction")
        void whenOrderAlreadyProcessed_shouldReturnFalseAndSkip() {
            String orderNumber = "ORD-ALREADY-DONE";
            when(processedOrderRepository.existsByOrderNumber(orderNumber)).thenReturn(true);

            boolean result = inventoryService.processOrderStockReduction(orderNumber, List.of(
                    new OrderCreatedEvent.OrderItemEvent("SKU1", "100.00", 1)
            ));

            assertThat(result).isFalse();
            verify(processedOrderRepository).existsByOrderNumber(orderNumber);
            verifyNoInteractions(inventoryRepository);
        }

        @Test
        @DisplayName("When items are null or empty, should throw IllegalArgumentException")
        void whenItemsNullOrEmpty_shouldThrowIllegalArgumentException() {
            String orderNumber = "ORD-EMPTY";
            when(processedOrderRepository.existsByOrderNumber(orderNumber)).thenReturn(false);

            assertThatThrownBy(() -> inventoryService.processOrderStockReduction(orderNumber, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order items cannot be empty");

            assertThatThrownBy(() -> inventoryService.processOrderStockReduction(orderNumber, Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order items cannot be empty");
        }

        @Test
        @DisplayName("When allowBackorders is true, should bypass stock check and record processed order")
        void whenAllowBackordersTrue_shouldBypassStockCheck() {
            ReflectionTestUtils.setField(inventoryService, "allowBackOrders", true);
            String orderNumber = "ORD-BACKORDER";
            when(processedOrderRepository.existsByOrderNumber(orderNumber)).thenReturn(false);

            List<OrderCreatedEvent.OrderItemEvent> items = List.of(
                    new OrderCreatedEvent.OrderItemEvent("SKU1", "50.00", 2)
            );

            boolean result = inventoryService.processOrderStockReduction(orderNumber, items);

            assertThat(result).isTrue();
            verifyNoInteractions(inventoryRepository);
            verify(processedOrderRepository).save(any(ProcessedOrder.class));
        }

        @Test
        @DisplayName("When stock is sufficient for all items, should deduct and record processed order")
        void whenStockSufficient_shouldDeductAllItems() {
            String orderNumber = "ORD-SUCCESS";
            when(processedOrderRepository.existsByOrderNumber(orderNumber)).thenReturn(false);

            Inventory inv1 = InventoryFactory.createCustomInventory(1L, "SKU_B", 20);
            Inventory inv2 = InventoryFactory.createCustomInventory(2L, "SKU_A", 10);

            when(inventoryRepository.findBySkuWithLock("SKU_A")).thenReturn(Optional.of(inv2));
            when(inventoryRepository.findBySkuWithLock("SKU_B")).thenReturn(Optional.of(inv1));

            // items passed in reverse order to verify alphabetical sorting
            List<OrderCreatedEvent.OrderItemEvent> items = List.of(
                    new OrderCreatedEvent.OrderItemEvent("SKU_B", "50.00", 5),
                    new OrderCreatedEvent.OrderItemEvent("SKU_A", "100.00", 3)
            );

            boolean result = inventoryService.processOrderStockReduction(orderNumber, items);

            assertThat(result).isTrue();
            assertThat(inv2.getQuantity()).isEqualTo(7);
            assertThat(inv1.getQuantity()).isEqualTo(15);
            verify(inventoryRepository).save(inv2);
            verify(inventoryRepository).save(inv1);
            verify(processedOrderRepository).save(any(ProcessedOrder.class));
        }

        @Test
        @DisplayName("When one item has insufficient stock, should throw InsufficientStockException")
        void whenOneItemInsufficientStock_shouldThrowInsufficientStockException() {
            String orderNumber = "ORD-FAIL-STOCK";
            when(processedOrderRepository.existsByOrderNumber(orderNumber)).thenReturn(false);

            Inventory inv1 = InventoryFactory.createCustomInventory(1L, "SKU_A", 2);
            when(inventoryRepository.findBySkuWithLock("SKU_A")).thenReturn(Optional.of(inv1));

            List<OrderCreatedEvent.OrderItemEvent> items = List.of(
                    new OrderCreatedEvent.OrderItemEvent("SKU_A", "100.00", 5)
            );

            assertThatThrownBy(() -> inventoryService.processOrderStockReduction(orderNumber, items))
                    .isInstanceOf(InsufficientStockException.class);

            verify(processedOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("When one item is not found, should throw ResourceNotFoundException")
        void whenOneItemNotFound_shouldThrowResourceNotFoundException() {
            String orderNumber = "ORD-NOT-FOUND";
            when(processedOrderRepository.existsByOrderNumber(orderNumber)).thenReturn(false);

            when(inventoryRepository.findBySkuWithLock("SKU_UNKNOWN")).thenReturn(Optional.empty());

            List<OrderCreatedEvent.OrderItemEvent> items = List.of(
                    new OrderCreatedEvent.OrderItemEvent("SKU_UNKNOWN", "100.00", 1)
            );

            assertThatThrownBy(() -> inventoryService.processOrderStockReduction(orderNumber, items))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(processedOrderRepository, never()).save(any());
        }
    }
}
