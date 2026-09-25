package br.com.spolador.ecommerce.order_service;

import br.com.spolador.ecommerce.order_service.config.InstallOpenTelemetryAppender;
import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.order_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.listener.OrderEventListener;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import br.com.spolador.ecommerce.order_service.model.OutboxEvent;
import br.com.spolador.ecommerce.order_service.repository.OrderRepository;
import br.com.spolador.ecommerce.order_service.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("OrderService Integration Tests")
class OrderIntegrationTest {

    private static final String BASE_PATH = "/api/v1/order";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderEventListener orderEventListener;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @MockitoBean
    private br.com.spolador.ecommerce.order_service.repository.RegisteredProductRepository registeredProductRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private OpenTelemetry openTelemetry;

    @MockitoBean
    private Tracer tracer;

    @MockitoBean
    private InstallOpenTelemetryAppender installOpenTelemetryAppender;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        Tracer noopTracer = OpenTelemetry.noop().getTracer("test");
        when(tracer.spanBuilder(anyString())).thenAnswer(inv -> noopTracer.spanBuilder(inv.getArgument(0)));
        when(openTelemetry.getTracer(anyString())).thenReturn(noopTracer);

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        TransactionStatus txStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(txStatus);
        when(registeredProductRepository.existsBySku(anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("Security & Authentication Integration")
    class SecurityIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated POST request")
        void givenUnauthenticated_whenCreateOrder_thenReturns401() throws Exception {
            OrderRequestDTO requestDTO = OrderFactory.createOrderRequestDTO();

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated GET request")
        void givenUnauthenticated_whenGetOrders_thenReturns401() throws Exception {
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated DELETE request")
        void givenUnauthenticated_whenDeleteOrder_thenReturns401() throws Exception {
            mockMvc.perform(delete(BASE_PATH + "/{id}", 1L))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST " + BASE_PATH + " - Create Order Integration")
    class CreateOrderIntegrationTests {

        @Test
        @DisplayName("Should create order, publish event, save to outbox, and return 201 Created")
        void givenAuthenticatedUserAndValidRequest_whenCreateOrder_thenReturns201() throws Exception {
            OrderRequestDTO requestDTO = OrderFactory.createOrderRequestDTO();
            Order savedOrder = OrderFactory.createOrder();
            savedOrder.setId(10L);

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().jwt(j -> j.subject("user-123")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.orderNumber").value(OrderFactory.DEFAULT_ORDER_NUMBER))
                    .andExpect(jsonPath("$.orderStatus").value("CREATED"))
                    .andExpect(jsonPath("$.orderLineItemList", hasSize(1)));

            verify(orderRepository).save(any(Order.class));
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.created"), any(OrderCreatedEvent.class));
            verify(outboxRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when request fails validation")
        void givenInvalidRequest_whenCreateOrder_thenReturns400() throws Exception {
            OrderRequestDTO invalidDTO = new OrderRequestDTO(Collections.emptyList(), "");

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().jwt(j -> j.subject("user-123")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Validation error"));

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when SKU is not registered")
        void givenUnregisteredSku_whenCreateOrder_thenReturns400() throws Exception {
            OrderRequestDTO requestDTO = OrderFactory.createOrderRequestDTO();
            when(registeredProductRepository.existsBySku(anyString())).thenReturn(false);

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().jwt(j -> j.subject("user-123")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Product Not Registered"));

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + " - Retrieve Orders Integration")
    class GetOrdersIntegrationTests {

        @Test
        @DisplayName("Should return user-specific orders when authenticated as regular user")
        void givenRegularUser_whenGetOrders_thenReturnsUserOrders() throws Exception {
            Order order = OrderFactory.createOrder();
            when(orderRepository.findByUserId("user-123")).thenReturn(List.of(order));

            mockMvc.perform(get(BASE_PATH)
                            .with(jwt().jwt(j -> j.subject("user-123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].orderNumber").value(OrderFactory.DEFAULT_ORDER_NUMBER));

            verify(orderRepository).findByUserId("user-123");
            verify(orderRepository, never()).findAll();
        }

        @Test
        @DisplayName("Should return all orders when authenticated as admin user")
        void givenAdminUser_whenGetOrders_thenReturnsAllOrders() throws Exception {
            Order order1 = OrderFactory.createOrder();
            Order order2 = OrderFactory.createOrder();
            order2.setId(2L);
            order2.setOrderNumber("second-order-number");

            when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

            mockMvc.perform(get(BASE_PATH)
                            .with(jwt().jwt(j -> j.subject("admin-user")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].orderNumber").value(OrderFactory.DEFAULT_ORDER_NUMBER))
                    .andExpect(jsonPath("$[1].orderNumber").value("second-order-number"));

            verify(orderRepository).findAll();
            verify(orderRepository, never()).findByUserId(anyString());
        }

        @Test
        @DisplayName("Should return order details when ID exists")
        void givenExistingId_whenGetOrderById_thenReturnsOrder() throws Exception {
            Order order = OrderFactory.createOrder();
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            mockMvc.perform(get(BASE_PATH + "/{id}", 1L)
                            .with(jwt().jwt(j -> j.subject("user-123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.orderNumber").value(OrderFactory.DEFAULT_ORDER_NUMBER));

            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user tries to access another user's order")
        void givenOtherUser_whenGetOrderById_thenReturns403() throws Exception {
            Order order = OrderFactory.createOrder(); // owner is user-123
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            mockMvc.perform(get(BASE_PATH + "/{id}", 1L)
                            .with(jwt().jwt(j -> j.subject("intruder-user"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.detail").value("You are not authorized to view this order"));

            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return order details when admin accesses another user's order")
        void givenAdminUser_whenGetOrderById_thenReturnsOrder() throws Exception {
            Order order = OrderFactory.createOrder(); // owner is user-123
            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

            mockMvc.perform(get(BASE_PATH + "/{id}", 1L)
                            .with(jwt().jwt(j -> j.subject("admin-user")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.orderNumber").value(OrderFactory.DEFAULT_ORDER_NUMBER));

            verify(orderRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found when order ID does not exist")
        void givenNonExistingId_whenGetOrderById_thenReturns404() throws Exception {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_PATH + "/{id}", 999L)
                            .with(jwt().jwt(j -> j.subject("user-123"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Resource not found."))
                    .andExpect(jsonPath("$.detail").value("Order not found with id: '999'"));

            verify(orderRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_PATH + "/{id} - Delete Order Integration")
    class DeleteOrderIntegrationTests {

        @Test
        @DisplayName("Should delete order and return 204 No Content when admin deletes existing ID")
        void givenAdminUser_whenDeleteOrder_thenReturns204() throws Exception {
            when(orderRepository.existsById(1L)).thenReturn(true);
            doNothing().when(orderRepository).deleteById(1L);

            mockMvc.perform(delete(BASE_PATH + "/{id}", 1L)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("admin-user")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isNoContent());

            verify(orderRepository).existsById(1L);
            verify(orderRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should return 403 Forbidden when regular user tries to delete an order")
        void givenRegularUser_whenDeleteOrder_thenReturns403() throws Exception {
            mockMvc.perform(delete(BASE_PATH + "/{id}", 1L)
                            .with(jwt().jwt(j -> j.subject("user-123")
                                    .claim("realm_access", Map.of("roles", List.of("USER"))))))
                    .andExpect(status().isForbidden());

            verify(orderRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should return 404 Not Found when admin deletes non-existing ID")
        void givenAdminUser_whenDeletingNonExistingId_thenReturns404() throws Exception {
            when(orderRepository.existsById(999L)).thenReturn(false);

            mockMvc.perform(delete(BASE_PATH + "/{id}", 999L)
                            .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("admin-user")
                                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Order not found with id: '999'"));

            verify(orderRepository).existsById(999L);
            verify(orderRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Event-Driven Integration - OrderEventListener")
    class OrderEventListenerIntegrationTests {

        @Test
        @DisplayName("Should update order status to CONFIRMED when OrderConfirmedEvent is handled")
        void givenOrderConfirmedEvent_whenHandleOrderConfirmed_thenUpdatesStatusToConfirmed() {
            OrderConfirmedEvent event = OrderFactory.createOrderConfirmedEvent();
            Order order = OrderFactory.createOrder();
            order.setOrderStatus(OrderStatus.CREATED);

            when(orderRepository.findByOrderNumber(event.orderNumber())).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            orderEventListener.handleOrderConfirmed(event);

            verify(orderRepository).findByOrderNumber(event.orderNumber());
            verify(orderRepository).save(argThat(savedOrder -> savedOrder.getOrderStatus() == OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Should update order status to CANCELLED when OrderCancelledEvent is handled")
        void givenOrderCancelledEvent_whenHandleOrderCancelled_thenUpdatesStatusToCancelled() {
            OrderCancelledEvent event = OrderFactory.createOrderCancelledEvent("Insufficient stock");
            Order order = OrderFactory.createOrder();
            order.setOrderStatus(OrderStatus.CREATED);

            when(orderRepository.findByOrderNumber(event.orderNumber())).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            orderEventListener.handleOrderCancelled(event);

            verify(orderRepository).findByOrderNumber(event.orderNumber());
            verify(orderRepository).save(argThat(savedOrder -> savedOrder.getOrderStatus() == OrderStatus.CANCELLED
                    && "Insufficient stock".equals(savedOrder.getCancellationReason())));
        }
    }
}
