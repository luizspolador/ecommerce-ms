package br.com.spolador.ecommerce.order_service.factory;

import br.com.spolador.ecommerce.order_service.dto.OrderLineItemRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderLineItemResponseDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.order_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.model.OrderLineItems;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class OrderFactory {

    private OrderFactory() {
    }

    public static final Long DEFAULT_ID = 1L;
    public static final String DEFAULT_ORDER_NUMBER = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    public static final String DEFAULT_USER_ID = "user-123";
    public static final String DEFAULT_EMAIL = "customer@ecommerce.com";
    public static final String DEFAULT_SKU = "IPHONE_15_BLACK";
    public static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(999.99);
    public static final Integer DEFAULT_QUANTITY = 2;

    public static OrderLineItems createOrderLineItem() {
        return OrderLineItems.builder()
                .id(DEFAULT_ID)
                .sku(DEFAULT_SKU)
                .price(DEFAULT_PRICE)
                .quantity(DEFAULT_QUANTITY)
                .build();
    }

    public static OrderLineItems createOrderLineItemWithoutId() {
        return OrderLineItems.builder()
                .sku(DEFAULT_SKU)
                .price(DEFAULT_PRICE)
                .quantity(DEFAULT_QUANTITY)
                .build();
    }

    public static Order createOrder() {
        return Order.builder()
                .id(DEFAULT_ID)
                .orderNumber(DEFAULT_ORDER_NUMBER)
                .userId(DEFAULT_USER_ID)
                .orderStatus(OrderStatus.CREATED)
                .orderLineItemList(new ArrayList<>(List.of(createOrderLineItem())))
                .build();
    }

    public static Order createOrderWithoutId() {
        return Order.builder()
                .orderLineItemList(new ArrayList<>(List.of(createOrderLineItemWithoutId())))
                .build();
    }

    public static OrderLineItemRequestDTO createOrderLineItemRequestDTO() {
        return new OrderLineItemRequestDTO(DEFAULT_SKU, DEFAULT_PRICE, DEFAULT_QUANTITY);
    }

    public static OrderRequestDTO createOrderRequestDTO() {
        return new OrderRequestDTO(
                List.of(createOrderLineItemRequestDTO()),
                DEFAULT_EMAIL
        );
    }

    public static OrderLineItemResponseDTO createOrderLineItemResponseDTO() {
        return new OrderLineItemResponseDTO(DEFAULT_ID, DEFAULT_SKU, DEFAULT_PRICE, DEFAULT_QUANTITY);
    }

    public static OrderResponseDTO createOrderResponseDTO() {
        return OrderResponseDTO.builder()
                .id(DEFAULT_ID)
                .orderNumber(DEFAULT_ORDER_NUMBER)
                .orderStatus(OrderStatus.CREATED)
                .orderLineItemList(List.of(createOrderLineItemResponseDTO()))
                .build();
    }

    public static OutboxEvent createOutboxEvent(boolean processed) {
        return OutboxEvent.builder()
                .id(DEFAULT_ID)
                .aggregateId(DEFAULT_ORDER_NUMBER)
                .type("ORDER_CREATED")
                .payload("{\"orderNumber\":\"" + DEFAULT_ORDER_NUMBER + "\",\"email\":\"" + DEFAULT_EMAIL + "\",\"items\":[]}")
                .createdAt(LocalDateTime.now())
                .processed(processed)
                .build();
    }

    public static OrderCreatedEvent createOrderCreatedEvent() {
        return new OrderCreatedEvent(
                DEFAULT_ORDER_NUMBER,
                DEFAULT_EMAIL,
                List.of(new OrderCreatedEvent.OrderItemEvent(DEFAULT_SKU, DEFAULT_PRICE.toString(), DEFAULT_QUANTITY))
        );
    }

    public static OrderConfirmedEvent createOrderConfirmedEvent() {
        return new OrderConfirmedEvent(DEFAULT_ORDER_NUMBER, DEFAULT_EMAIL);
    }

    public static OrderCancelledEvent createOrderCancelledEvent(String reason) {
        return new OrderCancelledEvent(DEFAULT_ORDER_NUMBER, DEFAULT_EMAIL, reason);
    }
}
