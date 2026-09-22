package br.com.spolador.ecommerce.product_service.service.impl;

import br.com.spolador.ecommerce.product_service.dto.ProductRequestDTO;
import br.com.spolador.ecommerce.product_service.dto.ProductResponseDTO;
import br.com.spolador.ecommerce.product_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.product_service.factory.ProductFactory;
import br.com.spolador.ecommerce.product_service.mapper.ProductMapper;
import br.com.spolador.ecommerce.product_service.model.Product;
import br.com.spolador.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ProductServiceImpl")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ProductServiceImpl productService;

    @Nested
    @DisplayName("createProduct()")
    class CreateProductTests {

        @Test
        @DisplayName("Given valid ProductRequestDTO, should persist, publish event to RabbitMQ and return ProductResponseDTO")
        void givenValidRequestDTO_whenCreateProduct_shouldReturnResponseDTO() {
            // Arrange
            ProductRequestDTO requestDTO = ProductFactory.createProductRequestDTO();
            Product productToSave = ProductFactory.createProductWithoutId();
            Product savedProduct = ProductFactory.createProduct();
            ProductResponseDTO expectedResponseDTO = ProductFactory.createProductResponseDTO();

            when(productMapper.toProduct(requestDTO)).thenReturn(productToSave);
            when(productRepository.save(productToSave)).thenReturn(savedProduct);
            when(productMapper.toProductResponseDTO(savedProduct)).thenReturn(expectedResponseDTO);

            // Act
            ProductResponseDTO actualResponseDTO = productService.createProduct(requestDTO);

            // Assert
            assertThat(actualResponseDTO).isNotNull();
            assertThat(actualResponseDTO.id()).isEqualTo(expectedResponseDTO.id());
            assertThat(actualResponseDTO.sku()).isEqualTo(expectedResponseDTO.sku());
            assertThat(actualResponseDTO.name()).isEqualTo(expectedResponseDTO.name());
            assertThat(actualResponseDTO.description()).isEqualTo(expectedResponseDTO.description());
            assertThat(actualResponseDTO.price()).isEqualTo(expectedResponseDTO.price());

            verify(productMapper).toProduct(requestDTO);
            verify(productRepository).save(productToSave);
            verify(productMapper).toProductResponseDTO(savedProduct);
            verify(rabbitTemplate).convertAndSend(eq("product-events"), eq("product.created"), any(br.com.spolador.ecommerce.product_service.event.ProductCreatedEvent.class));
            verifyNoMoreInteractions(productRepository, productMapper, rabbitTemplate);
        }
    }

    @Nested
    @DisplayName("getAllProducts()")
    class GetAllProductsTests {

        @Test
        @DisplayName("Given existing products, should return list of ProductResponseDTO")
        void givenExistingProducts_whenGetAllProducts_shouldReturnResponseDTOList() {
            // Arrange
            List<Product> products = ProductFactory.createProductList();
            ProductResponseDTO response1 = ProductFactory.createProductResponseDTO();
            ProductResponseDTO response2 = ProductFactory.createCustomProductResponseDTO("65f1a2b3c4d5e6f7a8b9c0d2", "Wireless Earbuds", "Noise-cancelling earbuds", BigDecimal.valueOf(199.99));
            ProductResponseDTO response3 = ProductFactory.createCustomProductResponseDTO("65f1a2b3c4d5e6f7a8b9c0d3", "Smart Watch", "Fitness tracking smartwatch", BigDecimal.valueOf(299.99));

            when(productRepository.findAll()).thenReturn(products);
            when(productMapper.toProductResponseDTO(products.get(0))).thenReturn(response1);
            when(productMapper.toProductResponseDTO(products.get(1))).thenReturn(response2);
            when(productMapper.toProductResponseDTO(products.get(2))).thenReturn(response3);

            // Act
            List<ProductResponseDTO> result = productService.getAllProducts();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).containsExactly(response1, response2, response3);

            verify(productRepository).findAll();
            verify(productMapper, times(3)).toProductResponseDTO(any(Product.class));
            verifyNoMoreInteractions(productRepository, productMapper);
        }

        @Test
        @DisplayName("Given no products, should return empty list")
        void givenNoProducts_whenGetAllProducts_shouldReturnEmptyList() {
            // Arrange
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<ProductResponseDTO> result = productService.getAllProducts();

            // Assert
            assertThat(result).isNotNull().isEmpty();

            verify(productRepository).findAll();
            verifyNoInteractions(productMapper);
            verifyNoMoreInteractions(productRepository);
        }
    }

    @Nested
    @DisplayName("getProductById()")
    class GetProductByIdTests {

        @Test
        @DisplayName("Given existing product ID, should return ProductResponseDTO")
        void givenExistingId_whenGetProductById_shouldReturnProductResponseDTO() {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            Product product = ProductFactory.createProduct();
            ProductResponseDTO expectedResponse = ProductFactory.createProductResponseDTO();

            when(productRepository.findById(id)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponseDTO(product)).thenReturn(expectedResponse);

            // Act
            ProductResponseDTO actualResponse = productService.getProductById(id);

            // Assert
            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.id()).isEqualTo(id);
            assertThat(actualResponse.name()).isEqualTo(expectedResponse.name());

            verify(productRepository).findById(id);
            verify(productMapper).toProductResponseDTO(product);
            verifyNoMoreInteractions(productRepository, productMapper);
        }

        @Test
        @DisplayName("Given non-existing product ID, should throw ResourceNotFoundException")
        void givenNonExistingId_whenGetProductById_shouldThrowResourceNotFoundException() {
            // Arrange
            String nonExistingId = "invalid-id";
            when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.getProductById(nonExistingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found with id: 'invalid-id'");

            verify(productRepository).findById(nonExistingId);
            verifyNoInteractions(productMapper);
            verifyNoMoreInteractions(productRepository);
        }
    }

    @Nested
    @DisplayName("updateProduct()")
    class UpdateProductTests {

        @Test
        @DisplayName("Given existing product ID and valid request, should update and return updated ProductResponseDTO")
        void givenExistingIdAndValidRequest_whenUpdateProduct_shouldReturnUpdatedResponseDTO() {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            Product existingProduct = ProductFactory.createProduct();
            ProductRequestDTO updateRequest = ProductFactory.createCustomProductRequestDTO("Updated Smartphone", "Updated Description", BigDecimal.valueOf(1799.99));
            Product updatedProduct = ProductFactory.createCustomProduct(id, "Updated Smartphone", "Updated Description", BigDecimal.valueOf(1799.99));
            ProductResponseDTO updatedResponseDTO = ProductFactory.createCustomProductResponseDTO(id, "Updated Smartphone", "Updated Description", BigDecimal.valueOf(1799.99));

            when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));
            doNothing().when(productMapper).updateProductFromRequest(updateRequest, existingProduct);
            when(productRepository.save(existingProduct)).thenReturn(updatedProduct);
            when(productMapper.toProductResponseDTO(updatedProduct)).thenReturn(updatedResponseDTO);

            // Act
            ProductResponseDTO result = productService.updateProduct(id, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("Updated Smartphone");
            assertThat(result.description()).isEqualTo("Updated Description");
            assertThat(result.price()).isEqualTo(BigDecimal.valueOf(1799.99));

            verify(productRepository).findById(id);
            verify(productMapper).updateProductFromRequest(updateRequest, existingProduct);
            verify(productRepository).save(existingProduct);
            verify(productMapper).toProductResponseDTO(updatedProduct);
            verifyNoMoreInteractions(productRepository, productMapper);
        }

        @Test
        @DisplayName("Given non-existing product ID, should throw ResourceNotFoundException and not save")
        void givenNonExistingId_whenUpdateProduct_shouldThrowResourceNotFoundException() {
            // Arrange
            String nonExistingId = "invalid-id";
            ProductRequestDTO updateRequest = ProductFactory.createProductRequestDTO();
            when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(nonExistingId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found with id: 'invalid-id'");

            verify(productRepository).findById(nonExistingId);
            verifyNoInteractions(productMapper);
            verify(productRepository, never()).save(any());
            verifyNoMoreInteractions(productRepository);
        }
    }

    @Nested
    @DisplayName("deleteProductById()")
    class DeleteProductByIdTests {

        @Test
        @DisplayName("Given existing product ID, should delete product from repository")
        void givenExistingId_whenDeleteProductById_shouldDeleteProduct() {
            // Arrange
            String id = ProductFactory.DEFAULT_ID;
            when(productRepository.existsById(id)).thenReturn(true);
            doNothing().when(productRepository).deleteById(id);

            // Act
            productService.deleteProductById(id);

            // Assert
            verify(productRepository).existsById(id);
            verify(productRepository).deleteById(id);
            verifyNoMoreInteractions(productRepository);
        }

        @Test
        @DisplayName("Given non-existing product ID, should throw ResourceNotFoundException and not delete")
        void givenNonExistingId_whenDeleteProductById_shouldThrowResourceNotFoundException() {
            // Arrange
            String nonExistingId = "invalid-id";
            when(productRepository.existsById(nonExistingId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProductById(nonExistingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found with id: 'invalid-id'");

            verify(productRepository).existsById(nonExistingId);
            verify(productRepository, never()).deleteById(any());
            verifyNoMoreInteractions(productRepository);
        }
    }
}
