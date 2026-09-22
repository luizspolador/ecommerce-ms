package br.com.spolador.ecommerce.inventory_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INVENTORY_QUEUE = "inventory-queue";
    public static final String ORDER_EVENTS_EXCHANGE = "order-events";
    public static final String INVENTORY_DLX = "inventory-dlx";
    public static final String INVENTORY_DLQ = "inventory-dlq";
    public static final String INVENTORY_DEAD_ROUTING_KEY = "inventory.dead";

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(INVENTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", INVENTORY_DLX)
                .withArgument("x-dead-letter-routing-key", INVENTORY_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue inventoryQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(inventoryQueue).to(orderEventsExchange).with("order.created");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(INVENTORY_DLX);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(INVENTORY_DLQ, true);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(INVENTORY_DEAD_ROUTING_KEY);
    }
}
