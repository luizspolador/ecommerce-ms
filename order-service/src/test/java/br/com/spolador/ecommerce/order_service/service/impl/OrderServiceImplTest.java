package br.com.spolador.ecommerce.order_service.service.impl;

import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.order_service.exception.ServiceUnavailableException;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.mapper.OrderMapper;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;
import br.com.spolador.ecommerce.order_service.repository.OrderRepository;
import br.com.spolador.ecommerce.order_service.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OrderServiceImpl")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OutboxService outboxService;

    @Mock
    private br.com.spolador.ecommerce.order_service.repository.RegisteredProductRepository registeredProductRepository;

    @InjectMocks
    private OrderServiceImpl orderService;


    @Nested
    @DisplayName("createOrder()")
    class CreateOrderTests {

        @Test
        @DisplayName("When orderEnabled is false, should throw ServiceUnavailableException")
        void whenOrderDisabled_shouldThrowException() {
            ReflectionTestUtils.setField(orderService, "orderEnabled", false);
            OrderRequestDTO request = OrderFactory.createOrderRequestDTO();

            assertThatThrownBy(() -> orderService.createOrder(request, "user-1"))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessageContaining("The ordering service is under maintenance");

            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("When sku is not registered, should throw ProductNotRegisteredException")
        void whenSkuNotRegistered_shouldThrowException() {
            ReflectionTestUtils.setField(orderService, "orderEnabled", true);
            OrderRequestDTO request = OrderFactory.createOrderRequestDTO();
            when(registeredProductRepository.existsBySku(anyString())).thenReturn(false);

            assertThatThrownBy(() -> orderService.createOrder(request, "user-1"))
                    .isInstanceOf(br.com.spolador.ecommerce.order_service.exception.ProductNotRegisteredException.class)
                    .hasMessageContaining("is not registered in catalog");

            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("When orderEnabled is true, should persist order, save to outbox with processed=false, publish domain event, and return DTO")
        void whenOrderEnabled_shouldCreateOrder() {
            ReflectionTestUtils.setField(orderService, "orderEnabled", true);
            OrderRequestDTO request = OrderFactory.createOrderRequestDTO();
            Order orderToSave = OrderFactory.createOrderWithoutId();
            Order savedOrder = OrderFactory.createOrder();
            OrderResponseDTO expectedResponse = OrderFactory.createOrderResponseDTO();
            OutboxEvent outboxEvent = OrderFactory.createOutboxEvent(false);

            when(registeredProductRepository.existsBySku(anyString())).thenReturn(true);
            when(orderMapper.toOrder(request)).thenReturn(orderToSave);
            when(orderRepository.save(orderToSave)).thenReturn(savedOrder);
            when(orderMapper.toOrderResponse(savedOrder)).thenReturn(expectedResponse);
            when(outboxService.saveOrderCreatedEvent(any(OrderCreatedEvent.class), eq(false))).thenReturn(outboxEvent);

            OrderResponseDTO actualResponse = orderService.createOrder(request, "user-1");

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(orderRepository).save(orderToSave);
            verify(outboxService).saveOrderCreatedEvent(any(OrderCreatedEvent.class), eq(false));
            verify(eventPublisher).publishEvent(any(br.com.spolador.ecommerce.order_service.event.OrderCreatedDomainEvent.class));
        }

        @Test
        @DisplayName("When outbox returns null event, should still publish domain event with null id and return DTO")
        void whenOutboxReturnsNull_shouldStillPublishDomainEvent() {
            ReflectionTestUtils.setField(orderService, "orderEnabled", true);
            OrderRequestDTO request = OrderFactory.createOrderRequestDTO();
            Order orderToSave = OrderFactory.createOrderWithoutId();
            Order savedOrder = OrderFactory.createOrder();
            OrderResponseDTO expectedResponse = OrderFactory.createOrderResponseDTO();

            when(registeredProductRepository.existsBySku(anyString())).thenReturn(true);
            when(orderMapper.toOrder(request)).thenReturn(orderToSave);
            when(orderRepository.save(orderToSave)).thenReturn(savedOrder);
            when(orderMapper.toOrderResponse(savedOrder)).thenReturn(expectedResponse);
            when(outboxService.saveOrderCreatedEvent(any(OrderCreatedEvent.class), eq(false))).thenReturn(null);

            OrderResponseDTO actualResponse = orderService.createOrder(request, "user-1");

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(orderRepository).save(orderToSave);
            verify(outboxService).saveOrderCreatedEvent(any(OrderCreatedEvent.class), eq(false));
            verify(eventPublisher).publishEvent(any(br.com.spolador.ecommerce.order_service.event.OrderCreatedDomainEvent.class));
        }
    }

    @Nested
    @DisplayName("getOrders()")
    class GetOrdersTests {

        @Test
        @DisplayName("When isAdmin is true, should find all orders")
        void whenAdmin_shouldFindAll() {
            Order order = OrderFactory.createOrder();
            when(orderRepository.findAll()).thenReturn(List.of(order));
            when(orderMapper.toOrderResponse(order)).thenReturn(OrderFactory.createOrderResponseDTO());

            List<OrderResponseDTO> result = orderService.getOrders("admin-id", true);

            assertThat(result).hasSize(1);
            verify(orderRepository).findAll();
            verify(orderRepository, never()).findByUserId(anyString());
        }

        @Test
        @DisplayName("When isAdmin is false, should find orders by userId")
        void whenNotAdmin_shouldFindByUserId() {
            Order order = OrderFactory.createOrder();
            when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));
            when(orderMapper.toOrderResponse(order)).thenReturn(OrderFactory.createOrderResponseDTO());

            List<OrderResponseDTO> result = orderService.getOrders("user-1", false);

            assertThat(result).hasSize(1);
            verify(orderRepository).findByUserId("user-1");
            verify(orderRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("getOrderById()")
    class GetOrderByIdTests {

        @Test
        @DisplayName("When order found, should return OrderResponseDTO")
        void whenFound_shouldReturnDTO() {
            Order order = OrderFactory.createOrder();
            OrderResponseDTO expectedResponse = OrderFactory.createOrderResponseDTO();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderResponse(order)).thenReturn(expectedResponse);

            OrderResponseDTO actualResponse = orderService.getOrderById(1L);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("When order not found, should throw ResourceNotFoundException")
        void whenNotFound_shouldThrowException() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found with id: '999'");

            verify(orderRepository).findById(999L);
        }

        @Test
        @DisplayName("When user is the owner of the order, should return OrderResponseDTO")
        void whenUserIsOwner_shouldReturnDTO() {
            Order order = OrderFactory.createOrder();
            order.setUserId("user-owner");
            OrderResponseDTO expectedResponse = OrderFactory.createOrderResponseDTO();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderResponse(order)).thenReturn(expectedResponse);

            OrderResponseDTO actualResponse = orderService.getOrderById(1L, "user-owner", false);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("When user is admin, should return OrderResponseDTO even if not the owner")
        void whenUserIsAdmin_shouldReturnDTO() {
            Order order = OrderFactory.createOrder();
            order.setUserId("user-owner");
            OrderResponseDTO expectedResponse = OrderFactory.createOrderResponseDTO();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(orderMapper.toOrderResponse(order)).thenReturn(expectedResponse);

            OrderResponseDTO actualResponse = orderService.getOrderById(1L, "admin-user", true);

            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("When user is neither owner nor admin, should throw AccessDeniedException")
        void whenUserIsNotOwnerAndNotAdmin_shouldThrowAccessDeniedException() {
            Order order = OrderFactory.createOrder();
            order.setUserId("user-owner");
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderById(1L, "other-user", false))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                    .hasMessageContaining("You are not authorized to view this order");

            verify(orderRepository).findById(1L);
            verifyNoInteractions(orderMapper);
        }
    }

    @Nested
    @DisplayName("deleteOrder()")
    class DeleteOrderTests {

        @Test
        @DisplayName("When order exists, should delete")
        void whenExists_shouldDelete() {
            when(orderRepository.existsById(1L)).thenReturn(true);

            orderService.deleteOrder(1L);

            verify(orderRepository).existsById(1L);
            verify(orderRepository).deleteById(1L);
        }

        @Test
        @DisplayName("When order does not exist, should throw ResourceNotFoundException")
        void whenDoesNotExist_shouldThrowException() {
            when(orderRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> orderService.deleteOrder(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found with id: '999'");

            verify(orderRepository).existsById(999L);
            verify(orderRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("updateOrderStatus()")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("When order found, should update status and save")
        void whenOrderFound_shouldUpdateStatus() {
            Order order = OrderFactory.createOrder();
            when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

            orderService.updateOrderStatus(order.getOrderNumber(), OrderStatus.CONFIRMED);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).findByOrderNumber(order.getOrderNumber());
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("When order found and reason provided, should update status, set reason, and save")
        void whenOrderFoundWithReason_shouldUpdateStatusAndReason() {
            Order order = OrderFactory.createOrder();
            when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

            orderService.updateOrderStatus(order.getOrderNumber(), OrderStatus.CANCELLED, "Insufficient stock");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancellationReason()).isEqualTo("Insufficient stock");
            verify(orderRepository).findByOrderNumber(order.getOrderNumber());
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("When order already has target status, should skip saving for idempotency")
        void whenStatusAlreadyMatches_shouldSkipSave() {
            Order order = OrderFactory.createOrder();
            order.setOrderStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

            orderService.updateOrderStatus(order.getOrderNumber(), OrderStatus.CONFIRMED);

            verify(orderRepository).findByOrderNumber(order.getOrderNumber());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("When order not found, should log error and not save")
        void whenOrderNotFound_shouldNotSave() {
            when(orderRepository.findByOrderNumber("UNKNOWN")).thenReturn(Optional.empty());

            orderService.updateOrderStatus("UNKNOWN", OrderStatus.CANCELLED);

            verify(orderRepository).findByOrderNumber("UNKNOWN");
            verify(orderRepository, never()).save(any());
        }
    }
}
