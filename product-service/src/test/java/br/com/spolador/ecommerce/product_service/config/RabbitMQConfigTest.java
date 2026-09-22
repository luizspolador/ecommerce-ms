package br.com.spolador.ecommerce.product_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for RabbitMQConfig in product-service")
class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Should instantiate messageConverter as JacksonJsonMessageConverter")
    void testMessageConverter() {
        MessageConverter converter = rabbitMQConfig.messageConverter();
        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    @DisplayName("Should create productEventsExchange with name 'product-events'")
    void testProductEventsExchange() {
        TopicExchange exchange = rabbitMQConfig.productEventsExchange();
        assertThat(exchange.getName()).isEqualTo("product-events");
    }
}
