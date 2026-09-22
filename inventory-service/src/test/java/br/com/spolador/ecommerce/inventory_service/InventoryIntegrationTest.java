package br.com.spolador.ecommerce.inventory_service;

import br.com.spolador.ecommerce.inventory_service.config.InstallOpenTelemetryAppender;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.inventory_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import br.com.spolador.ecommerce.inventory_service.listener.OrderEventListener;
import br.com.spolador.ecommerce.inventory_service.model.Inventory;
import br.com.spolador.ecommerce.inventory_service.repository.InventoryRepository;
import br.com.spolador.ecommerce.inventory_service.repository.ProcessedOrderRepository;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.List;
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
@DisplayName("InventoryService Integration Tests")
class InventoryIntegrationTest {

    private static final String BASE_PATH = "/api/v1/inventory";

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
    private InventoryRepository inventoryRepository;

    @MockitoBean
    private ProcessedOrderRepository processedOrderRepository;

    @MockitoBean
    private br.com.spolador.ecommerce.inventory_service.repository.RegisteredProductRepository registeredProductRepository;

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
    @DisplayName("GET " + BASE_PATH + "/{sku} - Check Stock Integration")
    class CheckStockIntegrationTests {

        @Test
        @DisplayName("Should return 200 OK and true when SKU exists and quantity is sufficient without authentication")
        void givenSufficientStock_whenCheckStock_thenReturnsTrue() throws Exception {
            Inventory inventory = InventoryFactory.createInventory();
            when(inventoryRepository.findBySku(InventoryFactory.DEFAULT_SKU)).thenReturn(Optional.of(inventory));

            mockMvc.perform(get(BASE_PATH + "/{sku}", InventoryFactory.DEFAULT_SKU)
                            .param("quantity", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(inventoryRepository).findBySku(InventoryFactory.DEFAULT_SKU);
        }

        @Test
        @DisplayName("Should return 200 OK and false when SKU exists but quantity is insufficient")
        void givenInsufficientStock_whenCheckStock_thenReturnsFalse() throws Exception {
            Inventory inventory = InventoryFactory.createCustomInventory(1L, InventoryFactory.DEFAULT_SKU, 5);
            when(inventoryRepository.findBySku(InventoryFactory.DEFAULT_SKU)).thenReturn(Optional.of(inventory));

            mockMvc.perform(get(BASE_PATH + "/{sku}", InventoryFactory.DEFAULT_SKU)
                            .param("quantity", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(inventoryRepository).findBySku(InventoryFactory.DEFAULT_SKU);
        }

        @Test
        @DisplayName("Should return 200 OK and false when SKU does not exist")
        void givenNonExistingSku_whenCheckStock_thenReturnsFalse() throws Exception {
            when(inventoryRepository.findBySku("UNKNOWN_SKU")).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_PATH + "/{sku}", "UNKNOWN_SKU")
                            .param("quantity", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(inventoryRepository).findBySku("UNKNOWN_SKU");
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + " - Get All Inventories Integration")
    class GetAllInventoriesIntegrationTests {

        @Test
        @DisplayName("Should return 200 OK and list of inventories without authentication")
        void givenExistingInventories_whenGetAllInventories_thenReturnsList() throws Exception {
            List<Inventory> inventories = InventoryFactory.createInventoryList();
            when(inventoryRepository.findAll()).thenReturn(inventories);

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").value(InventoryFactory.DEFAULT_ID))
                    .andExpect(jsonPath("$[0].sku").value(InventoryFactory.DEFAULT_SKU))
                    .andExpect(jsonPath("$[0].inStock").value(true));

            verify(inventoryRepository).findAll();
        }
    }

    @Nested
    @DisplayName("POST " + BASE_PATH + " - Create Inventory Integration")
    class CreateInventoryIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenCreateInventory_thenReturns401() throws Exception {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when caller is regular USER")
        void givenRegularUser_whenCreateInventory_thenReturns403() throws Exception {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should create stock and return 201 Created for valid payload when admin")
        void givenValidPayload_whenCreateInventory_thenReturns201() throws Exception {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();
            Inventory savedInventory = InventoryFactory.createInventory();

            when(inventoryRepository.existsBySku(InventoryFactory.DEFAULT_SKU)).thenReturn(false);
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(savedInventory);

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(InventoryFactory.DEFAULT_ID))
                    .andExpect(jsonPath("$.sku").value(InventoryFactory.DEFAULT_SKU))
                    .andExpect(jsonPath("$.quantity").value(InventoryFactory.DEFAULT_QUANTITY))
                    .andExpect(jsonPath("$.inStock").value(true));

            verify(inventoryRepository).existsBySku(InventoryFactory.DEFAULT_SKU);
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when product SKU is not registered in catalog")
        void givenUnregisteredProduct_whenCreateInventory_thenReturns400BadRequest() throws Exception {
            InventoryRequestDTO requestDTO = InventoryFactory.createCustomInventoryRequestDTO("UNREGISTERED_SKU", 10);
            when(registeredProductRepository.existsBySku("UNREGISTERED_SKU")).thenReturn(false);

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Product Not Registered"))
                    .andExpect(jsonPath("$.detail").value("Product with SKU 'UNREGISTERED_SKU' is not registered in catalog. You can only create inventory for registered products."))
                    .andExpect(jsonPath("$.Sku").value("UNREGISTERED_SKU"));

            verify(registeredProductRepository).existsBySku("UNREGISTERED_SKU");
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when payload fails validation")
        void givenInvalidPayload_whenCreateInventory_thenReturns400() throws Exception {
            InventoryRequestDTO invalidDTO = new InventoryRequestDTO("", -5);

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Validation error"));

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict when SKU already exists")
        void givenDuplicateSku_whenCreateInventory_thenReturns409Conflict() throws Exception {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();
            when(inventoryRepository.existsBySku(InventoryFactory.DEFAULT_SKU)).thenReturn(true);

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"))
                    .andExpect(jsonPath("$.detail").value("The inventory for SKU 'IPHONE_15_BLACK' already exists"))
                    .andExpect(jsonPath("$.Sku").value("IPHONE_15_BLACK"));

            verify(inventoryRepository).existsBySku(InventoryFactory.DEFAULT_SKU);
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_PATH + "/{id} - Update Inventory Integration")
    class UpdateInventoryIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenUpdateInventory_thenReturns401() throws Exception {
            InventoryRequestDTO updateDTO = InventoryFactory.createCustomInventoryRequestDTO("IPHONE_15_BLACK", 100);

            mockMvc.perform(put(BASE_PATH + "/{id}", InventoryFactory.DEFAULT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isUnauthorized());

            verify(inventoryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when regular user tries to update")
        void givenRegularUser_whenUpdateInventory_thenReturns403() throws Exception {
            InventoryRequestDTO updateDTO = InventoryFactory.createCustomInventoryRequestDTO("IPHONE_15_BLACK", 100);

            mockMvc.perform(put(BASE_PATH + "/{id}", InventoryFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isForbidden());

            verify(inventoryRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should update inventory and return 200 OK when admin and ID exists")
        void givenExistingId_whenUpdateInventory_thenReturns200() throws Exception {
            Inventory existing = InventoryFactory.createInventory();
            InventoryRequestDTO updateDTO = InventoryFactory.createCustomInventoryRequestDTO("IPHONE_15_BLACK", 100);
            Inventory updated = InventoryFactory.createCustomInventory(InventoryFactory.DEFAULT_ID, "IPHONE_15_BLACK", 100);

            when(inventoryRepository.findById(InventoryFactory.DEFAULT_ID)).thenReturn(Optional.of(existing));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(updated);

            mockMvc.perform(put(BASE_PATH + "/{id}", InventoryFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sku").value("IPHONE_15_BLACK"))
                    .andExpect(jsonPath("$.quantity").value(100));

            verify(inventoryRepository).findById(InventoryFactory.DEFAULT_ID);
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating non-existing ID")
        void givenNonExistingId_whenUpdateInventory_thenReturns404() throws Exception {
            InventoryRequestDTO updateDTO = InventoryFactory.createInventoryRequestDTO();
            when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put(BASE_PATH + "/{id}", 999L)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Resource not found."))
                    .andExpect(jsonPath("$.detail").value("Inventory not found with id: '999'"));

            verify(inventoryRepository).findById(999L);
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_PATH + "/reduce/{sku} - Reduce Stock Integration")
    class ReduceStockIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenReduceStock_thenReturns401() throws Exception {
            mockMvc.perform(put(BASE_PATH + "/reduce/{sku}", "IPHONE_15_BLACK")
                            .param("quantity", "5"))
                    .andExpect(status().isUnauthorized());

            verify(inventoryRepository, never()).findBySkuWithLock(anyString());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when regular user tries to reduce stock")
        void givenRegularUser_whenReduceStock_thenReturns403() throws Exception {
            mockMvc.perform(put(BASE_PATH + "/reduce/{sku}", "IPHONE_15_BLACK")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                            .param("quantity", "5"))
                    .andExpect(status().isForbidden());

            verify(inventoryRepository, never()).findBySkuWithLock(anyString());
        }

        @Test
        @DisplayName("Should reduce stock and return 200 OK when admin, SKU exists and quantity available")
        void givenExistingSkuAndSufficientQuantity_whenReduceStock_thenReturns200() throws Exception {
            Inventory existing = InventoryFactory.createCustomInventory(1L, "IPHONE_15_BLACK", 20);
            when(inventoryRepository.findBySkuWithLock("IPHONE_15_BLACK")).thenReturn(Optional.of(existing));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put(BASE_PATH + "/reduce/{sku}", "IPHONE_15_BLACK")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .param("quantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Stock was reduced"));

            verify(inventoryRepository).findBySkuWithLock("IPHONE_15_BLACK");
            verify(inventoryRepository).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when reducing more stock than available")
        void givenInsufficientQuantity_whenReduceStock_thenReturns400() throws Exception {
            Inventory existing = InventoryFactory.createCustomInventory(1L, "IPHONE_15_BLACK", 2);
            when(inventoryRepository.findBySkuWithLock("IPHONE_15_BLACK")).thenReturn(Optional.of(existing));

            mockMvc.perform(put(BASE_PATH + "/reduce/{sku}", "IPHONE_15_BLACK")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .param("quantity", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Insufficient stock"))
                    .andExpect(jsonPath("$.detail").value("Insufficient stock for SKU 'IPHONE_15_BLACK'. Requested: 10, Available: 2"));

            verify(inventoryRepository).findBySkuWithLock("IPHONE_15_BLACK");
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_PATH + "/{id} - Delete Inventory Integration")
    class DeleteInventoryIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenDeleteInventory_thenReturns401() throws Exception {
            mockMvc.perform(delete(BASE_PATH + "/{id}", InventoryFactory.DEFAULT_ID))
                    .andExpect(status().isUnauthorized());

            verify(inventoryRepository, never()).existsById(anyLong());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when regular user tries to delete")
        void givenRegularUser_whenDeleteInventory_thenReturns403() throws Exception {
            mockMvc.perform(delete(BASE_PATH + "/{id}", InventoryFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                    .andExpect(status().isForbidden());

            verify(inventoryRepository, never()).existsById(anyLong());
        }

        @Test
        @DisplayName("Should delete inventory and return 204 No Content when admin deletes existing ID")
        void givenExistingId_whenDeleteInventory_thenReturns204() throws Exception {
            when(inventoryRepository.existsById(InventoryFactory.DEFAULT_ID)).thenReturn(true);
            doNothing().when(inventoryRepository).deleteById(InventoryFactory.DEFAULT_ID);

            mockMvc.perform(delete(BASE_PATH + "/{id}", InventoryFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNoContent());

            verify(inventoryRepository).existsById(InventoryFactory.DEFAULT_ID);
            verify(inventoryRepository).deleteById(InventoryFactory.DEFAULT_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when deleting non-existing ID")
        void givenNonExistingId_whenDeleteInventory_thenReturns404() throws Exception {
            when(inventoryRepository.existsById(999L)).thenReturn(false);

            mockMvc.perform(delete(BASE_PATH + "/{id}", 999L)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Inventory not found with id: '999'"));

            verify(inventoryRepository).existsById(999L);
            verify(inventoryRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Event-Driven Integration - OrderEventListener")
    class OrderEventListenerIntegrationTests {

        @Test
        @DisplayName("Should confirm order and deduct stock when all products are in stock")
        void givenInStockProducts_whenHandleOrderCreatedEvent_thenConfirmsOrderAndDeductsStock() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            Inventory item1 = InventoryFactory.createCustomInventory(1L, InventoryFactory.DEFAULT_SKU, 50);
            Inventory item2 = InventoryFactory.createCustomInventory(2L, "GALAXY_S24_ULTRA", 25);

            when(inventoryRepository.findBySkuWithLock(InventoryFactory.DEFAULT_SKU)).thenReturn(Optional.of(item1));
            when(inventoryRepository.findBySkuWithLock("GALAXY_S24_ULTRA")).thenReturn(Optional.of(item2));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

            orderEventListener.handleOrderCreatedEvent(event);

            verify(inventoryRepository, atLeastOnce()).save(any(Inventory.class));
            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.confirmed"), any(OrderConfirmedEvent.class));
            verify(rabbitTemplate, never()).convertAndSend(eq("order-events"), eq("order.cancelled"), any(OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("Should cancel order when any product is out of stock")
        void givenOutOfStockProducts_whenHandleOrderCreatedEvent_thenCancelsOrder() {
            OrderCreatedEvent event = InventoryFactory.createOrderCreatedEvent();
            Inventory item1 = InventoryFactory.createCustomInventory(1L, InventoryFactory.DEFAULT_SKU, 0); // Out of stock

            when(inventoryRepository.findBySkuWithLock(InventoryFactory.DEFAULT_SKU)).thenReturn(Optional.of(item1));

            orderEventListener.handleOrderCreatedEvent(event);

            verify(rabbitTemplate).convertAndSend(eq("order-events"), eq("order.cancelled"), any(OrderCancelledEvent.class));
            verify(rabbitTemplate, never()).convertAndSend(eq("order-events"), eq("order.confirmed"), any(OrderConfirmedEvent.class));
        }
    }
}
