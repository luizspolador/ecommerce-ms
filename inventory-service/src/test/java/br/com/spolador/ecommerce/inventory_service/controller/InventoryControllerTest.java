package br.com.spolador.ecommerce.inventory_service.controller;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.exception.GlobalControllerAdvice;
import br.com.spolador.ecommerce.inventory_service.factory.InventoryFactory;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for InventoryController")
class InventoryControllerTest {

    private static final String BASE_PATH = "/api/v1/inventory";

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalControllerAdvice())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + "/{sku}?quantity={quantity} (isInStock)")
    class IsInStockTests {

        @Test
        @DisplayName("Should return 200 OK and true when stock available")
        void shouldReturnTrueWhenInStock() throws Exception {
            when(inventoryService.isInStock("SKU-1", 5)).thenReturn(true);

            mockMvc.perform(get(BASE_PATH + "/{sku}", "SKU-1")
                            .param("quantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(inventoryService).isInStock("SKU-1", 5);
        }

        @Test
        @DisplayName("Should return 200 OK and false when stock not available")
        void shouldReturnFalseWhenOutOfStock() throws Exception {
            when(inventoryService.isInStock("SKU-1", 100)).thenReturn(false);

            mockMvc.perform(get(BASE_PATH + "/{sku}", "SKU-1")
                            .param("quantity", "100"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(inventoryService).isInStock("SKU-1", 100);
        }
    }

    @Nested
    @DisplayName("POST " + BASE_PATH + " (createInventory)")
    class CreateInventoryTests {

        @Test
        @DisplayName("Should return 201 Created when request is valid")
        void shouldReturnCreatedWhenValid() throws Exception {
            InventoryRequestDTO requestDTO = InventoryFactory.createInventoryRequestDTO();
            InventoryResponseDTO responseDTO = InventoryFactory.createInventoryResponseDTO();

            when(inventoryService.createInventory(any(InventoryRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(responseDTO.getId()))
                    .andExpect(jsonPath("$.sku").value(responseDTO.getSku()))
                    .andExpect(jsonPath("$.quantity").value(responseDTO.getQuantity()))
                    .andExpect(jsonPath("$.inStock").value(responseDTO.isInStock()));

            verify(inventoryService).createInventory(any(InventoryRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when SKU is blank")
        void shouldReturnBadRequestWhenSkuBlank() throws Exception {
            InventoryRequestDTO invalidDTO = new InventoryRequestDTO("", 10);

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation error"))
                    .andExpect(jsonPath("$.errors.sku").exists());

            verifyNoInteractions(inventoryService);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when quantity is negative")
        void shouldReturnBadRequestWhenQuantityNegative() throws Exception {
            InventoryRequestDTO invalidDTO = new InventoryRequestDTO("SKU-1", -1);

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation error"))
                    .andExpect(jsonPath("$.errors.quantity").exists());

            verifyNoInteractions(inventoryService);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + " (getAllInventories)")
    class GetAllInventoriesTests {

        @Test
        @DisplayName("Should return 200 OK and list of inventories")
        void shouldReturnAllInventories() throws Exception {
            List<InventoryResponseDTO> list = List.of(
                    InventoryFactory.createInventoryResponseDTO(),
                    InventoryResponseDTO.builder().id(2L).sku("SKU-2").quantity(10).inStock(true).build()
            );
            when(inventoryService.getAllInventories()).thenReturn(list);

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].sku").value(InventoryFactory.DEFAULT_SKU));

            verify(inventoryService).getAllInventories();
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_PATH + "/{id} (updateInventory)")
    class UpdateInventoryTests {

        @Test
        @DisplayName("Should return 200 OK when update is valid")
        void shouldReturnOkWhenValid() throws Exception {
            Long id = 1L;
            InventoryRequestDTO requestDTO = new InventoryRequestDTO("SKU-UPDATED", 30);
            InventoryResponseDTO responseDTO = InventoryResponseDTO.builder()
                    .id(id).sku("SKU-UPDATED").quantity(30).inStock(true).build();

            when(inventoryService.updateInventoryById(eq(id), any(InventoryRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(put(BASE_PATH + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.sku").value("SKU-UPDATED"))
                    .andExpect(jsonPath("$.quantity").value(30));

            verify(inventoryService).updateInventoryById(eq(id), any(InventoryRequestDTO.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when body is invalid")
        void shouldReturnBadRequestWhenInvalid() throws Exception {
            InventoryRequestDTO invalidDTO = new InventoryRequestDTO(null, -5);

            mockMvc.perform(put(BASE_PATH + "/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inventoryService);
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_PATH + "/reduce/{sku}?quantity={quantity} (reduceStock)")
    class ReduceStockTests {

        @Test
        @DisplayName("Should return 200 OK and success message")
        void shouldReduceStock() throws Exception {
            doNothing().when(inventoryService).reduceStock("SKU-1", 5);

            mockMvc.perform(put(BASE_PATH + "/reduce/{sku}", "SKU-1")
                            .param("quantity", "5"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Stock was reduced"));

            verify(inventoryService).reduceStock("SKU-1", 5);
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_PATH + "/{id} (deleteInventory)")
    class DeleteInventoryTests {

        @Test
        @DisplayName("Should return 204 No Content when deleted")
        void shouldReturnNoContent() throws Exception {
            doNothing().when(inventoryService).deleteInventoryById(1L);

            mockMvc.perform(delete(BASE_PATH + "/{id}", 1L))
                    .andExpect(status().isNoContent());

            verify(inventoryService).deleteInventoryById(1L);
        }
    }
}
