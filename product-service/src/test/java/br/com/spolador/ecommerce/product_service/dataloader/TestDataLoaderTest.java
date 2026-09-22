package br.com.spolador.ecommerce.product_service.dataloader;

import br.com.spolador.ecommerce.product_service.model.Product;
import br.com.spolador.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for TestDataLoader")
class TestDataLoaderTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private TestDataLoader testDataLoader;

    @Test
    @DisplayName("run should save initial test product in repository")
    void run_shouldSaveInitialProduct() throws Exception {
        // Act
        testDataLoader.run("arg1", "arg2");

        // Assert
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        Product saved = productCaptor.getValue();
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Smartphone");
        assertThat(saved.getDescription()).isEqualTo("beautiful smartphone");
        assertThat(saved.getPrice()).isEqualTo(BigDecimal.valueOf(1500));
    }
}
