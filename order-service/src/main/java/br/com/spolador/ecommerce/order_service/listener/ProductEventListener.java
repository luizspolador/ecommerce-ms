package br.com.spolador.ecommerce.order_service.listener;

import br.com.spolador.ecommerce.order_service.config.RabbitMQConfig;
import br.com.spolador.ecommerce.order_service.event.ProductCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.RegisteredProduct;
import br.com.spolador.ecommerce.order_service.repository.RegisteredProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {

    private final RegisteredProductRepository registeredProductRepository;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PRODUCT_QUEUE)
    @Transactional
    public void handleProductCreatedEvent(ProductCreatedEvent event) {
        log.info("ProductCreatedEvent received in OrderService for SKU: {}, Name: {}", event.sku(), event.name());
        registeredProductRepository.findBySku(event.sku()).ifPresentOrElse(
                existing -> {
                    existing.setName(event.name());
                    registeredProductRepository.save(existing);
                    log.info("Registered product updated for SKU: {}", event.sku());
                },
                () -> {
                    RegisteredProduct newProduct = RegisteredProduct.builder()
                            .sku(event.sku())
                            .name(event.name())
                            .registeredAt(LocalDateTime.now())
                            .build();
                    registeredProductRepository.save(newProduct);
                    log.info("New product registered in order-service catalog for SKU: {}", event.sku());
                }
        );
    }
}
