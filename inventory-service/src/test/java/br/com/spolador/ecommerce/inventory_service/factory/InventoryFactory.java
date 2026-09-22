package br.com.spolador.ecommerce.inventory_service.factory;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.model.Inventory;

import java.util.List;

public final class InventoryFactory {

    private InventoryFactory() {
    }

    public static final Long DEFAULT_ID = 1L;
    public static final String DEFAULT_SKU = "IPHONE_15_BLACK";
    public static final Integer DEFAULT_QUANTITY = 50;

    public static Inventory createInventory() {
        return Inventory.builder()
                .id(DEFAULT_ID)
                .sku(DEFAULT_SKU)
                .quantity(DEFAULT_QUANTITY)
                .build();
    }

    public static Inventory createInventoryWithoutId() {
        return Inventory.builder()
                .sku(DEFAULT_SKU)
                .quantity(DEFAULT_QUANTITY)
                .build();
    }

    public static Inventory createCustomInventory(Long id, String sku, Integer quantity) {
        return Inventory.builder()
                .id(id)
                .sku(sku)
                .quantity(quantity)
                .build();
    }

    public static InventoryRequestDTO createInventoryRequestDTO() {
        return new InventoryRequestDTO(DEFAULT_SKU, DEFAULT_QUANTITY);
    }

    public static InventoryRequestDTO createCustomInventoryRequestDTO(String sku, Integer quantity) {
        return new InventoryRequestDTO(sku, quantity);
    }

    public static InventoryResponseDTO createInventoryResponseDTO() {
        return InventoryResponseDTO.builder()
                .id(DEFAULT_ID)
                .sku(DEFAULT_SKU)
                .quantity(DEFAULT_QUANTITY)
                .inStock(true)
                .build();
    }

    public static List<Inventory> createInventoryList() {
        return List.of(
                createInventory(),
                createCustomInventory(2L, "GALAXY_S24_ULTRA", 25),
                createCustomInventory(3L, "MACBOOK_PRO_M3", 0)
        );
    }

    public static OrderCreatedEvent createOrderCreatedEvent() {
        return new OrderCreatedEvent(
                "ORD-12345",
                "customer@ecommerce.com",
                List.of(
                        new OrderCreatedEvent.OrderItemEvent(DEFAULT_SKU, "999.99", 2),
                        new OrderCreatedEvent.OrderItemEvent("GALAXY_S24_ULTRA", "899.99", 1)
                )
        );
    }

    public static OrderConfirmedEvent createOrderConfirmedEvent() {
        return new OrderConfirmedEvent("ORD-12345", "customer@ecommerce.com");
    }

    public static OrderCancelledEvent createOrderCancelledEvent(String reason) {
        return new OrderCancelledEvent("ORD-12345", "customer@ecommerce.com", reason);
    }
}
