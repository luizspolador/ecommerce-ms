package br.com.spolador.ecommerce.notification_service.config;

import br.com.spolador.ecommerce.notification_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.notification_service.event.OrderConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RabbitMQConfig Unit Tests")
class RabbitMQConfigTest {

    private RabbitMQConfig rabbitMQConfig;

    @BeforeEach
    void setUp() {
        rabbitMQConfig = new RabbitMQConfig();
    }

    @Nested
    @DisplayName("Message Converter Configuration")
    class MessageConverterTests {

        @Test
        @DisplayName("Should instantiate JacksonJsonMessageConverter and correctly deserialize OrderConfirmedEvent mapping")
        void givenOrderConfirmedMessage_whenFromMessage_thenDeserializesCorrectly() {
            MessageConverter converter = rabbitMQConfig.messageConverter();

            assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);

            MessageProperties properties = new MessageProperties();
            properties.setHeader("__TypeId__", "br.com.spolador.ecommerce.inventory_service.event.OrderConfirmedEvent");
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            String json = "{\"orderNumber\":\"ORD-001\",\"email\":\"user@example.com\"}";
            Message message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

            Object result = converter.fromMessage(message);

            assertThat(result).isInstanceOf(OrderConfirmedEvent.class);
            OrderConfirmedEvent event = (OrderConfirmedEvent) result;
            assertThat(event.orderNumber()).isEqualTo("ORD-001");
            assertThat(event.email()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should instantiate JacksonJsonMessageConverter and correctly deserialize OrderCancelledEvent mapping")
        void givenOrderCancelledMessage_whenFromMessage_thenDeserializesCorrectly() {
            MessageConverter converter = rabbitMQConfig.messageConverter();

            assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);

            MessageProperties properties = new MessageProperties();
            properties.setHeader("__TypeId__", "br.com.spolador.ecommerce.inventory_service.event.OrderCancelledEvent");
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            String json = "{\"orderNumber\":\"ORD-002\",\"email\":\"user@example.com\",\"reason\":\"Out of stock\"}";
            Message message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

            Object result = converter.fromMessage(message);

            assertThat(result).isInstanceOf(OrderCancelledEvent.class);
            OrderCancelledEvent event = (OrderCancelledEvent) result;
            assertThat(event.orderNumber()).isEqualTo("ORD-002");
            assertThat(event.email()).isEqualTo("user@example.com");
            assertThat(event.reason()).isEqualTo("Out of stock");
        }
    }

    @Nested
    @DisplayName("Queue and Exchange Declarations")
    class QueueAndExchangeTests {

        @Test
        @DisplayName("Should declare durable notificationQueue with dead letter arguments")
        void shouldDeclareNotificationQueue() {
            Queue queue = rabbitMQConfig.notificationQueue();

            assertThat(queue.getName()).isEqualTo("notification-queue");
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "notification-dlx");
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-routing-key", "notification.dead");
        }

        @Test
        @DisplayName("Should declare orderEventsExchange as TopicExchange")
        void shouldDeclareOrderEventsExchange() {
            TopicExchange exchange = rabbitMQConfig.orderEventsExchange();

            assertThat(exchange.getName()).isEqualTo("order-events");
            assertThat(exchange.getType()).isEqualTo("topic");
        }

        @Test
        @DisplayName("Should declare deadLetterExchange as DirectExchange")
        void shouldDeclareDeadLetterExchange() {
            DirectExchange exchange = rabbitMQConfig.deadLetterExchange();

            assertThat(exchange.getName()).isEqualTo("notification-dlx");
            assertThat(exchange.getType()).isEqualTo("direct");
        }

        @Test
        @DisplayName("Should declare deadLetterQueue as durable")
        void shouldDeclareDeadLetterQueue() {
            Queue queue = rabbitMQConfig.deadLetterQueue();

            assertThat(queue.getName()).isEqualTo("notification-dlq");
            assertThat(queue.isDurable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Binding Declarations")
    class BindingTests {

        @Test
        @DisplayName("Should bind notificationQueue to orderEventsExchange with routing key order.confirmed")
        void shouldBindConfirmedOrder() {
            Queue queue = rabbitMQConfig.notificationQueue();
            TopicExchange exchange = rabbitMQConfig.orderEventsExchange();

            Binding binding = rabbitMQConfig.binding(queue, exchange);

            assertThat(binding.getDestination()).isEqualTo("notification-queue");
            assertThat(binding.getExchange()).isEqualTo("order-events");
            assertThat(binding.getRoutingKey()).isEqualTo("order.confirmed");
        }

        @Test
        @DisplayName("Should bind notificationQueue to orderEventsExchange with routing key order.cancelled")
        void shouldBindCancelledOrder() {
            Queue queue = rabbitMQConfig.notificationQueue();
            TopicExchange exchange = rabbitMQConfig.orderEventsExchange();

            Binding binding = rabbitMQConfig.cancelledBinding(queue, exchange);

            assertThat(binding.getDestination()).isEqualTo("notification-queue");
            assertThat(binding.getExchange()).isEqualTo("order-events");
            assertThat(binding.getRoutingKey()).isEqualTo("order.cancelled");
        }

        @Test
        @DisplayName("Should bind deadLetterQueue to deadLetterExchange with routing key notification.dead")
        void shouldBindDeadLetterQueue() {
            Queue queue = rabbitMQConfig.deadLetterQueue();
            DirectExchange exchange = rabbitMQConfig.deadLetterExchange();

            Binding binding = rabbitMQConfig.deadLetterBinding(queue, exchange);

            assertThat(binding.getDestination()).isEqualTo("notification-dlq");
            assertThat(binding.getExchange()).isEqualTo("notification-dlx");
            assertThat(binding.getRoutingKey()).isEqualTo("notification.dead");
        }
    }
}
