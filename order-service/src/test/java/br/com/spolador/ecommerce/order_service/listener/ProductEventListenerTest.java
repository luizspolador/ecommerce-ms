package br.com.spolador.ecommerce.order_service.listener;

import br.com.spolador.ecommerce.order_service.event.ProductCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.RegisteredProduct;
import br.com.spolador.ecommerce.order_service.repository.RegisteredProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests for ProductEventListener in order-service")
class ProductEventListenerTest {

    @Mock
    private RegisteredProductRepository registeredProductRepository;

    @InjectMocks
    private ProductEventListener productEventListener;

    @Test
    @DisplayName("When product is not registered, should save new RegisteredProduct")
    void whenProductNotRegistered_shouldSaveNewRegisteredProduct() {
        ProductCreatedEvent event = new ProductCreatedEvent("p1", "SKU-TEST-01", "Gaming Mouse", BigDecimal.valueOf(150.0));
        when(registeredProductRepository.findBySku("SKU-TEST-01")).thenReturn(Optional.empty());

        productEventListener.handleProductCreatedEvent(event);

        ArgumentCaptor<RegisteredProduct> captor = ArgumentCaptor.forClass(RegisteredProduct.class);
        verify(registeredProductRepository).save(captor.capture());

        RegisteredProduct saved = captor.getValue();
        assertThat(saved.getSku()).isEqualTo("SKU-TEST-01");
        assertThat(saved.getName()).isEqualTo("Gaming Mouse");
        assertThat(saved.getRegisteredAt()).isNotNull();
    }

    @Test
    @DisplayName("When product is already registered, should update existing RegisteredProduct")
    void whenProductAlreadyRegistered_shouldUpdateExistingRegisteredProduct() {
        ProductCreatedEvent event = new ProductCreatedEvent("p1", "SKU-TEST-01", "Updated Gaming Mouse", BigDecimal.valueOf(180.0));
        RegisteredProduct existing = RegisteredProduct.builder()
                .id(1L)
                .sku("SKU-TEST-01")
                .name("Old Name")
                .registeredAt(LocalDateTime.now().minusDays(1))
                .build();

        when(registeredProductRepository.findBySku("SKU-TEST-01")).thenReturn(Optional.of(existing));

        productEventListener.handleProductCreatedEvent(event);

        verify(registeredProductRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("Updated Gaming Mouse");
    }
}
