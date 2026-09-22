package br.com.spolador.ecommerce.order_service.controller;

import br.com.spolador.ecommerce.order_service.dto.OrderLineItemRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.exception.GlobalControllerAdvice;
import br.com.spolador.ecommerce.order_service.factory.OrderFactory;
import br.com.spolador.ecommerce.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for OrderController")
class OrderControllerTest {

    private static final String BASE_PATH = "/api/v1/order";

    @Mock
    private OrderService orderService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalControllerAdvice())
                .build();
        objectMapper = new ObjectMapper();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(jwt, null));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST " + BASE_PATH + " (createOrder)")
    class CreateOrderTests {

        @Test
        @DisplayName("Should return 201 Created when order request is valid")
        void shouldCreateOrder() throws Exception {
            OrderRequestDTO requestDTO = OrderFactory.createOrderRequestDTO();
            OrderResponseDTO responseDTO = OrderFactory.createOrderResponseDTO();

            when(jwt.getSubject()).thenReturn("user-123");
            when(orderService.createOrder(any(OrderRequestDTO.class), eq("user-123"))).thenReturn(responseDTO);

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(responseDTO.getId()))
                    .andExpect(jsonPath("$.orderNumber").value(responseDTO.getOrderNumber()));

            verify(orderService).createOrder(any(OrderRequestDTO.class), eq("user-123"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid")
        void shouldReturnBadRequestWhenEmailInvalid() throws Exception {
            OrderRequestDTO invalidDTO = new OrderRequestDTO(
                    List.of(new OrderLineItemRequestDTO("SKU-1", BigDecimal.valueOf(10.0), 1)),
                    "not-an-email"
            );

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(orderService);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when items list is empty")
        void shouldReturnBadRequestWhenItemsEmpty() throws Exception {
            OrderRequestDTO invalidDTO = new OrderRequestDTO(
                    Collections.emptyList(),
                    "valid@email.com"
            );

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(orderService);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + " (getOrders)")
    class GetOrdersTests {

        @Test
        @DisplayName("When user has ADMIN role in realm_access, should call getOrders with isAdmin=true")
        void whenAdminRole_shouldCallWithIsAdminTrue() throws Exception {
            when(jwt.getSubject()).thenReturn("admin-user");
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("USER", "ADMIN")));
            when(orderService.getOrders("admin-user", true)).thenReturn(List.of(OrderFactory.createOrderResponseDTO()));

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(orderService).getOrders("admin-user", true);
        }

        @Test
        @DisplayName("When user does not have ADMIN role, should call getOrders with isAdmin=false")
        void whenNoAdminRole_shouldCallWithIsAdminFalse() throws Exception {
            when(jwt.getSubject()).thenReturn("regular-user");
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("USER")));
            when(orderService.getOrders("regular-user", false)).thenReturn(List.of(OrderFactory.createOrderResponseDTO()));

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(orderService).getOrders("regular-user", false);
        }

        @Test
        @DisplayName("When realm_access is null, should call getOrders with isAdmin=false")
        void whenRealmAccessNull_shouldCallWithIsAdminFalse() throws Exception {
            when(jwt.getSubject()).thenReturn("regular-user");
            when(jwt.getClaim("realm_access")).thenReturn(null);
            when(orderService.getOrders("regular-user", false)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(orderService).getOrders("regular-user", false);
        }

        @Test
        @DisplayName("When realm_access does not contain 'roles' key, should call getOrders with isAdmin=false")
        void whenRealmAccessWithoutRoles_shouldCallWithIsAdminFalse() throws Exception {
            when(jwt.getSubject()).thenReturn("regular-user");
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("other_key", "val"));
            when(orderService.getOrders("regular-user", false)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(orderService).getOrders("regular-user", false);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + "/{id} (getOrderById)")
    class GetOrderByIdTests {

        @Test
        @DisplayName("Should return 200 OK and OrderResponseDTO when authorized")
        void shouldReturnOrderById() throws Exception {
            OrderResponseDTO responseDTO = OrderFactory.createOrderResponseDTO();
            when(jwt.getSubject()).thenReturn("user-123");
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("USER")));
            when(orderService.getOrderById(1L, "user-123", false)).thenReturn(responseDTO);

            mockMvc.perform(get(BASE_PATH + "/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.orderNumber").value(responseDTO.getOrderNumber()));

            verify(orderService).getOrderById(1L, "user-123", false);
        }

        @Test
        @DisplayName("Should return 403 Forbidden when accessing another user's order without admin role")
        void whenAccessDenied_shouldReturn403() throws Exception {
            when(jwt.getSubject()).thenReturn("user-123");
            when(jwt.getClaim("realm_access")).thenReturn(null);
            when(orderService.getOrderById(1L, "user-123", false))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("You are not authorized to view this order"));

            mockMvc.perform(get(BASE_PATH + "/{id}", 1L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.title").value("Forbidden"))
                    .andExpect(jsonPath("$.detail").value("You are not authorized to view this order"));

            verify(orderService).getOrderById(1L, "user-123", false);
        }

        @Test
        @DisplayName("Should return 404 Not Found when order does not exist")
        void whenOrderNotFound_shouldReturn404() throws Exception {
            when(jwt.getSubject()).thenReturn("admin-user");
            when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("ADMIN")));
            when(orderService.getOrderById(999L, "admin-user", true))
                    .thenThrow(new br.com.spolador.ecommerce.order_service.exception.ResourceNotFoundException("Order", "id", 999L));

            mockMvc.perform(get(BASE_PATH + "/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));

            verify(orderService).getOrderById(999L, "admin-user", true);
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_PATH + "/{id} (deleteOrder)")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should return 204 No Content when deleted")
        void shouldDeleteOrder() throws Exception {
            doNothing().when(orderService).deleteOrder(1L);

            mockMvc.perform(delete(BASE_PATH + "/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(orderService).deleteOrder(1L);
        }
    }
}
