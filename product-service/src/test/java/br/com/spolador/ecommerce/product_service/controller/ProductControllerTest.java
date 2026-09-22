package br.com.spolador.ecommerce.product_service.controller;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;
import br.com.spolador.ecommerce.product_service.exception.GlobalControllerAdvice;
import br.com.spolador.ecommerce.product_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.product_service.factory.ProductFactory;
import br.com.spolador.ecommerce.product_service.service.ProductService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ProductController")
class ProductControllerTest {

    private static final String BASE_PATH = "/api/v1/product";

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productController, "maintenanceMessage", "operating system - test version");
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalControllerAdvice())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST " + BASE_PATH + " (createProduct)")
    class CreateProductTests {

        @Test
        @DisplayName("Given valid ProductRequestDTO, should return 201 Created and ProductResponseDTO")
        void givenValidRequestDTO_whenCreateProduct_shouldReturn201Created() throws Exception {
            // Arrange
            ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();
            ProductResponseDTO responseDTO = ProductFactory.createProductResponseDTO();

            when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(responseDTO.id())))
                    .andExpect(jsonPath("$.sku", is(responseDTO.sku())))
                    .andExpect(jsonPath("$.name", is(responseDTO.name())))
                    .andExpect(jsonPath("$.description", is(responseDTO.description())))
                    .andExpect(jsonPath("$.price", is(responseDTO.price().doubleValue())));

            verify(productService).createProduct(requestDTO);
        }

        @Test
        @DisplayName("Given blank sku, should return 400 Bad Request with validation error")
        void givenBlankSku_whenCreateProduct_shouldReturn400BadRequest() throws Exception {
            // Arrange
            ProductRequestDTO invalidRequest = ProductFactory.createInvalidProductRequestDTOWithBlankSku();

            // Act & Assert
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation error")))
                    .andExpect(jsonPath("$.errors.sku", is("product SKU is required")));

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("Given blank name, should return 400 Bad Request with validation error")
        void givenBlankName_whenCreateProduct_shouldReturn400BadRequest() throws Exception {
            // Arrange
            ProductRequestDTO invalidRequest = ProductFactory.createInvalidProductRequestDTOWithBlankName();

            // Act & Assert
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation error")))
                    .andExpect(jsonPath("$.errors.name", is("product name is required")));

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("Given null price, should return 400 Bad Request with validation error")
        void givenNullPrice_whenCreateProduct_shouldReturn400BadRequest() throws Exception {
            // Arrange
            ProductRequestDTO invalidRequest = ProductFactory.createInvalidProductRequestDTOWithNullPrice();

            // Act & Assert
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation error")))
                    .andExpect(jsonPath("$.errors.price", is("product price cannot be null")));

            verifyNoInteractions(productService);
        }

        @Test
        @DisplayName("Given negative price, should return 400 Bad Request with validation error")
        void givenNegativePrice_whenCreateProduct_shouldReturn400BadRequest() throws Exception {
            // Arrange
            ProductRequestDTO invalidRequest = ProductFactory.createInvalidProductRequestDTOWithNegativePrice();

            // Act & Assert
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation error")))
                    .andExpect(jsonPath("$.errors.price", is("product price must be greater than zero")));

            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + " (getAllProducts)")
    class GetAllProductsTests {

        @Test
        @DisplayName("When getAllProducts is called, should return 200 OK, products list, and maintenance header")
        void whenGetAllProducts_shouldReturn200AndListAndMaintenanceHeader() throws Exception {
            // Arrange
            List<ProductResponseDTO> responseList = ProductFactory.createProductResponseDTOList();
            when(productService.getAllProducts()).thenReturn(responseList);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Maintenance-Message", "operating system - test version"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(responseList.get(0).id())))
                    .andExpect(jsonPath("$[0].name", is(responseList.get(0).name())))
                    .andExpect(jsonPath("$[1].id", is(responseList.get(1).id())))
                    .andExpect(jsonPath("$[2].id", is(responseList.get(2).id())));

            verify(productService).getAllProducts();
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + "/{id} (getProductById)")
    class GetProductByIdTests {

        @Test
        @DisplayName("Given existing ID, should return 200 OK and ProductResponseDTO")
        void givenExistingId_whenGetProductById_shouldReturn200AndProduct() throws Exception {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            ProductResponseDTO responseDTO = ProductFactory.createProductResponseDTO();
            when(productService.getProductById(id)).thenReturn(responseDTO);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(id)))
                    .andExpect(jsonPath("$.name", is(responseDTO.name())))
                    .andExpect(jsonPath("$.description", is(responseDTO.description())))
                    .andExpect(jsonPath("$.price", is(responseDTO.price().doubleValue())));

            verify(productService).getProductById(id);
        }

        @Test
        @DisplayName("Given non-existing ID, should return 404 Not Found via ControllerAdvice")
        void givenNonExistingId_whenGetProductById_shouldReturn404NotFound() throws Exception {
            // Arrange
            String id = "invalid-id";
            when(productService.getProductById(id)).thenThrow(new ResourceNotFoundException("Product", "id", id));

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title", is("Resource not found.")))
                    .andExpect(jsonPath("$.detail", is("Product not found with id: 'invalid-id'")))
                    .andExpect(jsonPath("$.Resource", is("Product")))
                    .andExpect(jsonPath("$.Field", is("id")))
                    .andExpect(jsonPath("$.Value", is("invalid-id")));

            verify(productService).getProductById(id);
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_PATH + "/{id} (updateProductById)")
    class UpdateProductByIdTests {

        @Test
        @DisplayName("Given existing ID and valid request, should return 200 OK and updated ProductResponseDTO")
        void givenExistingIdAndValidRequest_whenUpdateProduct_shouldReturn200AndUpdatedProduct() throws Exception {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            ProductRequestDTO requestDTO = ProductFactory.createCustomProductRequestDTO("Updated Name", "Updated Desc", BigDecimal.valueOf(1800.00));
            ProductResponseDTO updatedResponse = ProductFactory.createCustomProductResponseDTO(id, "Updated Name", "Updated Desc", BigDecimal.valueOf(1800.00));

            when(productService.updateProduct(eq(id), any(ProductRequestDTO.class))).thenReturn(updatedResponse);

            // Act & Assert
            mockMvc.perform(put(BASE_PATH + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(id)))
                    .andExpect(jsonPath("$.name", is("Updated Name")))
                    .andExpect(jsonPath("$.description", is("Updated Desc")))
                    .andExpect(jsonPath("$.price", is(1800.00)));

            verify(productService).updateProduct(id, requestDTO);
        }

        @Test
        @DisplayName("Given non-existing ID, should return 404 Not Found")
        void givenNonExistingId_whenUpdateProduct_shouldReturn404NotFound() throws Exception {
            // Arrange
            String id = "invalid-id";
            ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();
            when(productService.updateProduct(eq(id), any(ProductRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Product", "id", id));

            // Act & Assert
            mockMvc.perform(put(BASE_PATH + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title", is("Resource not found.")));

            verify(productService).updateProduct(id, requestDTO);
        }

        @Test
        @DisplayName("Given invalid request body, should return 400 Bad Request")
        void givenInvalidBody_whenUpdateProduct_shouldReturn400BadRequest() throws Exception {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            ProductRequestDTO invalidRequest = ProductFactory.createInvalidProductRequestDTOWithBlankName();

            // Act & Assert
            mockMvc.perform(put(BASE_PATH + "/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Validation error")));

            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_PATH + "/{id} (deleteProductById)")
    class DeleteProductByIdTests {

        @Test
        @DisplayName("Given existing ID, should return 204 No Content")
        void givenExistingId_whenDeleteProductById_shouldReturn204NoContent() throws Exception {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            doNothing().when(productService).deleteProductById(id);

            // Act & Assert
            mockMvc.perform(delete(BASE_PATH + "/{id}", id))
                    .andExpect(status().isNoContent());

            verify(productService).deleteProductById(id);
        }

        @Test
        @DisplayName("Given non-existing ID, should return 404 Not Found")
        void givenNonExistingId_whenDeleteProductById_shouldReturn404NotFound() throws Exception {
            // Arrange
            String id = "invalid-id";
            doThrow(new ResourceNotFoundException("Product", "id", id)).when(productService).deleteProductById(id);

            // Act & Assert
            mockMvc.perform(delete(BASE_PATH + "/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title", is("Resource not found.")));

            verify(productService).deleteProductById(id);
        }
    }
}
