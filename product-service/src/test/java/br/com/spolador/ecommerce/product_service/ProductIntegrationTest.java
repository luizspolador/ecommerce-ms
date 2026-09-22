package br.com.spolador.ecommerce.product_service;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.factory.ProductFactory;
import br.com.spolador.ecommerce.product_service.model.Product;
import br.com.spolador.ecommerce.product_service.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("ProductService Integration Tests")
class ProductIntegrationTest {

    private static final String BASE_PATH = "/api/v1/product";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private MongoClient mongoClient;

    @MockitoBean
    private br.com.spolador.ecommerce.product_service.config.InstallOpenTelemetryAppender installOpenTelemetryAppender;

    @MockitoBean
    private io.opentelemetry.api.OpenTelemetry openTelemetry;

    @MockitoBean
    private io.opentelemetry.api.trace.Tracer tracer;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.OpenTelemetry.noop().getTracer("test");
        when(tracer.spanBuilder(anyString())).thenAnswer(inv -> noopTracer.spanBuilder(inv.getArgument(0)));
        when(openTelemetry.getTracer(anyString())).thenReturn(noopTracer);

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Nested
    @DisplayName("POST " + BASE_PATH + " - Product Creation Integration")
    class CreateProductIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenCreateProduct_thenReturns401() throws Exception {
            ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();

            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should return 403 Forbidden when caller is regular USER")
        void givenRegularUser_whenCreateProduct_thenReturns403() throws Exception {
            ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isForbidden());

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should create product and return 201 Created when caller is ADMIN and payload is valid")
        void givenValidRequest_whenCreateProduct_thenReturns201AndPersistedData() throws Exception {
            ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();
            Product savedProduct = ProductFactory.createProduct();

            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ProductFactory.DEFAULT_ID))
                    .andExpect(jsonPath("$.name").value(ProductFactory.DEFAULT_NAME))
                    .andExpect(jsonPath("$.description").value(ProductFactory.DEFAULT_DESCRIPTION))
                    .andExpect(jsonPath("$.price").value(ProductFactory.DEFAULT_PRICE));

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request with validation errors when request is invalid")
        void givenInvalidRequest_whenCreateProduct_thenReturns400() throws Exception {
            ProductRequestDTO invalidRequest = ProductFactory.createInvalidProductRequestDTOWithBlankName();

            mockMvc.perform(post(BASE_PATH)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH + " - Retrieve Products Integration")
    class GetProductsIntegrationTests {

        @Test
        @DisplayName("Should return 200 OK, list of products, and X-Maintenance-Message header without authentication")
        void givenExistingProducts_whenGetAllProducts_thenReturnsListAndHeader() throws Exception {
            List<Product> products = ProductFactory.createProductList();
            when(productRepository.findAll()).thenReturn(products);

            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Maintenance-Message", "test-maintenance-message"))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").value(ProductFactory.DEFAULT_ID))
                    .andExpect(jsonPath("$[0].name").value(ProductFactory.DEFAULT_NAME));

            verify(productRepository).findAll();
        }

        @Test
        @DisplayName("Should return 200 OK and product details when ID exists without authentication")
        void givenExistingId_whenGetProductById_thenReturnsProduct() throws Exception {
            Product product = ProductFactory.createProduct();
            when(productRepository.findById(ProductFactory.DEFAULT_ID)).thenReturn(Optional.of(product));

            mockMvc.perform(get(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ProductFactory.DEFAULT_ID))
                    .andExpect(jsonPath("$.name").value(ProductFactory.DEFAULT_NAME));

            verify(productRepository).findById(ProductFactory.DEFAULT_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when product ID does not exist")
        void givenNonExistingId_whenGetProductById_thenReturns404() throws Exception {
            when(productRepository.findById("non-existing-id")).thenReturn(Optional.empty());

            mockMvc.perform(get(BASE_PATH + "/{id}", "non-existing-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Resource not found."))
                    .andExpect(jsonPath("$.detail").value("Product not found with id: 'non-existing-id'"));

            verify(productRepository).findById("non-existing-id");
        }
    }

    @Nested
    @DisplayName("PUT " + BASE_PATH + "/{id} - Update Product Integration")
    class UpdateProductIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenUpdateProduct_thenReturns401() throws Exception {
            ProductRequestDTO updateRequest = ProductFactory.createProductRequestDTO();

            mockMvc.perform(put(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isUnauthorized());

            verify(productRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when regular user tries to update")
        void givenRegularUser_whenUpdateProduct_thenReturns403() throws Exception {
            ProductRequestDTO updateRequest = ProductFactory.createProductRequestDTO();

            mockMvc.perform(put(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());

            verify(productRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should update product and return 200 OK when admin and payload valid")
        void givenExistingIdAndValidPayload_whenUpdateProduct_thenReturnsUpdatedProduct() throws Exception {
            Product existingProduct = ProductFactory.createProduct();
            ProductRequestDTO updateRequest = ProductFactory.createCustomProductRequestDTO("Updated Name", "Updated Desc", BigDecimal.valueOf(1999.99));
            Product updatedProduct = ProductFactory.createCustomProduct(ProductFactory.DEFAULT_ID, "Updated Name", "Updated Desc", BigDecimal.valueOf(1999.99));

            when(productRepository.findById(ProductFactory.DEFAULT_ID)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

            mockMvc.perform(put(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"))
                    .andExpect(jsonPath("$.price").value(1999.99));

            verify(productRepository).findById(ProductFactory.DEFAULT_ID);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating non-existing product")
        void givenNonExistingId_whenUpdateProduct_thenReturns404() throws Exception {
            ProductRequestDTO updateRequest = ProductFactory.createProductRequestDTO();
            when(productRepository.findById("non-existing-id")).thenReturn(Optional.empty());

            mockMvc.perform(put(BASE_PATH + "/{id}", "non-existing-id")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Product not found with id: 'non-existing-id'"));

            verify(productRepository).findById("non-existing-id");
            verify(productRepository, never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("DELETE " + BASE_PATH + "/{id} - Delete Product Integration")
    class DeleteProductIntegrationTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when unauthenticated")
        void givenUnauthenticated_whenDeleteProduct_thenReturns401() throws Exception {
            mockMvc.perform(delete(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID))
                    .andExpect(status().isUnauthorized());

            verify(productRepository, never()).existsById(anyString());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when regular user tries to delete")
        void givenRegularUser_whenDeleteProduct_thenReturns403() throws Exception {
            mockMvc.perform(delete(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                    .andExpect(status().isForbidden());

            verify(productRepository, never()).existsById(anyString());
        }

        @Test
        @DisplayName("Should delete product and return 204 No Content when admin deletes existing ID")
        void givenExistingId_whenDeleteProduct_thenReturns204() throws Exception {
            when(productRepository.existsById(ProductFactory.DEFAULT_ID)).thenReturn(true);
            doNothing().when(productRepository).deleteById(ProductFactory.DEFAULT_ID);

            mockMvc.perform(delete(BASE_PATH + "/{id}", ProductFactory.DEFAULT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNoContent());

            verify(productRepository).existsById(ProductFactory.DEFAULT_ID);
            verify(productRepository).deleteById(ProductFactory.DEFAULT_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when deleting non-existing product")
        void givenNonExistingId_whenDeleteProduct_thenReturns404() throws Exception {
            when(productRepository.existsById("non-existing-id")).thenReturn(false);

            mockMvc.perform(delete(BASE_PATH + "/{id}", "non-existing-id")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Product not found with id: 'non-existing-id'"));

            verify(productRepository).existsById("non-existing-id");
            verify(productRepository, never()).deleteById(anyString());
        }
    }
}
