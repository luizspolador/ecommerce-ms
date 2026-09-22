package br.com.spolador.ecommerce.order_service.mapper;

import br.com.spolador.ecommerce.order_service.dto.OrderLineItemRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderLineItemResponseDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.model.OrderLineItems;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for OrderMapper")
class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapperImpl();
    }

    @Test
    @DisplayName("toOrder should map OrderRequestDTO to Order")
    void toOrder_shouldMapRequestToModel() {
        OrderRequestDTO requestDTO = OrderFactory.createOrderRequestDTO();

        Order order = orderMapper.toOrder(requestDTO);

        assertThat(order).isNotNull();
        assertThat(order.getOrderLineItemList()).hasSize(1);
        assertThat(order.getOrderLineItemList().get(0).getSku()).isEqualTo(OrderFactory.DEFAULT_SKU);
    }

    @Test
    @DisplayName("toOrder should return null when requestDTO is null")
    void toOrder_shouldReturnNullWhenNull() {
        assertThat(orderMapper.toOrder(null)).isNull();
    }

    @Test
    @DisplayName("toOrderLineItems should map OrderLineItemRequestDTO to OrderLineItems")
    void toOrderLineItems_shouldMapRequestToModel() {
        OrderLineItemRequestDTO itemRequest = OrderFactory.createOrderLineItemRequestDTO();

        OrderLineItems item = orderMapper.toOrderLineItems(itemRequest);

        assertThat(item).isNotNull();
        assertThat(item.getSku()).isEqualTo(itemRequest.getSku());
        assertThat(item.getPrice()).isEqualTo(itemRequest.getPrice());
        assertThat(item.getQuantity()).isEqualTo(itemRequest.getQuantity());
    }

    @Test
    @DisplayName("toOrderLineItems should return null when input is null")
    void toOrderLineItems_shouldReturnNullWhenNull() {
        assertThat(orderMapper.toOrderLineItems(null)).isNull();
    }

    @Test
    @DisplayName("toOrderResponse should map Order to OrderResponseDTO")
    void toOrderResponse_shouldMapModelToResponse() {
        Order order = OrderFactory.createOrder();

        OrderResponseDTO responseDTO = orderMapper.toOrderResponse(order);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getId()).isEqualTo(order.getId());
        assertThat(responseDTO.getOrderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(responseDTO.getOrderStatus()).isEqualTo(order.getOrderStatus());
        assertThat(responseDTO.getOrderLineItemList()).hasSize(1);
    }

    @Test
    @DisplayName("toOrderResponse should return null when order is null")
    void toOrderResponse_shouldReturnNullWhenNull() {
        assertThat(orderMapper.toOrderResponse(null)).isNull();
    }

    @Test
    @DisplayName("toOrderLineItemsResponse should map OrderLineItems to OrderLineItemResponseDTO")
    void toOrderLineItemsResponse_shouldMapModelToResponse() {
        OrderLineItems item = OrderFactory.createOrderLineItem();

        OrderLineItemResponseDTO responseDTO = orderMapper.toOrderLineItemsResponse(item);

        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getId()).isEqualTo(item.getId());
        assertThat(responseDTO.getSku()).isEqualTo(item.getSku());
        assertThat(responseDTO.getPrice()).isEqualTo(item.getPrice());
        assertThat(responseDTO.getQuantity()).isEqualTo(item.getQuantity());
    }

    @Test
    @DisplayName("toOrderLineItemsResponse should return null when item is null")
    void toOrderLineItemsResponse_shouldReturnNullWhenNull() {
        assertThat(orderMapper.toOrderLineItemsResponse(null)).isNull();
    }

    @Test
    @DisplayName("toOrder should handle null orderLineItemList in requestDTO")
    void toOrder_shouldHandleNullList() {
        OrderRequestDTO requestDTO = new OrderRequestDTO(null, "email@test.com");
        Order order = orderMapper.toOrder(requestDTO);
        assertThat(order).isNotNull();
        assertThat(order.getOrderLineItemList()).isNull();
    }

    @Test
    @DisplayName("toOrderResponse should handle null orderLineItemList in order")
    void toOrderResponse_shouldHandleNullList() {
        Order order = Order.builder().orderLineItemList(null).build();
        OrderResponseDTO responseDTO = orderMapper.toOrderResponse(order);
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getOrderLineItemList()).isNull();
    }
}
