package br.com.spolador.ecommerce.order_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit tests for RabbitMQConfig in order-service")
class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Should instantiate JacksonJsonMessageConverter")
    void testMessageConverter() {
        MessageConverter converter = rabbitMQConfig.messageConverter();
        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    @DisplayName("Should instantiate orderEventsExchange")
    void testOrderEventsExchange() {
        TopicExchange exchange = rabbitMQConfig.orderEventsExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_NAME);
    }

    @Test
    @DisplayName("Should instantiate orderConfirmedQueue and binding with DLQ arguments")
    void testOrderConfirmedQueueAndBinding() {
        Queue queue = rabbitMQConfig.orderConfirmedQueue();
        TopicExchange exchange = rabbitMQConfig.orderEventsExchange();
        Binding binding = rabbitMQConfig.confirmedBinding(queue, exchange);

        assertThat(queue.getName()).isEqualTo("order-confirmed-queue");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments().get("x-dead-letter-exchange")).isEqualTo("order-dlx");
        assertThat(queue.getArguments().get("x-dead-letter-routing-key")).isEqualTo("order.dead");
        assertThat(binding.getDestination()).isEqualTo("order-confirmed-queue");
        assertThat(binding.getRoutingKey()).isEqualTo("order.confirmed");
    }

    @Test
    @DisplayName("Should instantiate orderCancelledQueue and binding with DLQ arguments")
    void testOrderCancelledQueueAndBinding() {
        Queue queue = rabbitMQConfig.orderCancelledQueue();
        TopicExchange exchange = rabbitMQConfig.orderEventsExchange();
        Binding binding = rabbitMQConfig.cancelledBinding(queue, exchange);

        assertThat(queue.getName()).isEqualTo("order-cancelled-queue");
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments().get("x-dead-letter-exchange")).isEqualTo("order-dlx");
        assertThat(queue.getArguments().get("x-dead-letter-routing-key")).isEqualTo("order.dead");
        assertThat(binding.getDestination()).isEqualTo("order-cancelled-queue");
        assertThat(binding.getRoutingKey()).isEqualTo("order.cancelled");
    }

    @Test
    @DisplayName("Should create deadLetterExchange with name 'order-dlx'")
    void testDeadLetterExchange() {
        DirectExchange exchange = rabbitMQConfig.deadLetterExchange();
        assertThat(exchange.getName()).isEqualTo("order-dlx");
    }

    @Test
    @DisplayName("Should create deadLetterQueue with name 'order-dlq'")
    void testDeadLetterQueue() {
        Queue queue = rabbitMQConfig.deadLetterQueue();
        assertThat(queue.getName()).isEqualTo("order-dlq");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Should create binding between deadLetterQueue and deadLetterExchange with routing key 'order.dead'")
    void testDeadLetterBinding() {
        Queue queue = rabbitMQConfig.deadLetterQueue();
        DirectExchange exchange = rabbitMQConfig.deadLetterExchange();

        Binding binding = rabbitMQConfig.deadLetterBinding(queue, exchange);
        assertThat(binding.getDestination()).isEqualTo("order-dlq");
        assertThat(binding.getExchange()).isEqualTo("order-dlx");
        assertThat(binding.getRoutingKey()).isEqualTo("order.dead");
    }
}
