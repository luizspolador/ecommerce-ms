package br.com.spolador.ecommerce.inventory_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for RabbitMQConfig")
class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Should instantiate messageConverter as JacksonJsonMessageConverter")
    void testMessageConverter() {
        MessageConverter converter = rabbitMQConfig.messageConverter();
        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    @DisplayName("Should create durable inventoryQueue with name 'inventory-queue' and DLQ arguments")
    void testInventoryQueue() {
        Queue queue = rabbitMQConfig.inventoryQueue();
        assertThat(queue.getName()).isEqualTo("inventory-queue");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments().get("x-dead-letter-exchange")).isEqualTo("inventory-dlx");
        assertThat(queue.getArguments().get("x-dead-letter-routing-key")).isEqualTo("inventory.dead");
    }

    @Test
    @DisplayName("Should create orderEventsExchange with name 'order-events'")
    void testOrderEventsExchange() {
        TopicExchange exchange = rabbitMQConfig.orderEventsExchange();
        assertThat(exchange.getName()).isEqualTo("order-events");
    }

    @Test
    @DisplayName("Should create binding between queue and exchange with routing key 'order.created'")
    void testBinding() {
        Queue queue = rabbitMQConfig.inventoryQueue();
        TopicExchange exchange = rabbitMQConfig.orderEventsExchange();

        Binding binding = rabbitMQConfig.binding(queue, exchange);
        assertThat(binding.getDestination()).isEqualTo("inventory-queue");
        assertThat(binding.getExchange()).isEqualTo("order-events");
        assertThat(binding.getRoutingKey()).isEqualTo("order.created");
    }

    @Test
    @DisplayName("Should create deadLetterExchange with name 'inventory-dlx'")
    void testDeadLetterExchange() {
        DirectExchange exchange = rabbitMQConfig.deadLetterExchange();
        assertThat(exchange.getName()).isEqualTo("inventory-dlx");
    }

    @Test
    @DisplayName("Should create deadLetterQueue with name 'inventory-dlq'")
    void testDeadLetterQueue() {
        Queue queue = rabbitMQConfig.deadLetterQueue();
        assertThat(queue.getName()).isEqualTo("inventory-dlq");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Should create binding between deadLetterQueue and deadLetterExchange with routing key 'inventory.dead'")
    void testDeadLetterBinding() {
        Queue queue = rabbitMQConfig.deadLetterQueue();
        DirectExchange exchange = rabbitMQConfig.deadLetterExchange();

        Binding binding = rabbitMQConfig.deadLetterBinding(queue, exchange);
        assertThat(binding.getDestination()).isEqualTo("inventory-dlq");
        assertThat(binding.getExchange()).isEqualTo("inventory-dlx");
        assertThat(binding.getRoutingKey()).isEqualTo("inventory.dead");
    }
}
