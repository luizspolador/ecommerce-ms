package br.com.spolador.ecommerce.inventory_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INVENTORY_QUEUE = "inventory-queue";
    public static final String ORDER_EVENTS_EXCHANGE = "order-events";
    public static final String PRODUCT_EVENTS_EXCHANGE = "product-events";
    public static final String INVENTORY_PRODUCT_QUEUE = "inventory-product-queue";
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
    public Binding binding(@Qualifier("inventoryQueue") Queue inventoryQueue,
                           @Qualifier("orderEventsExchange") TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(inventoryQueue).to(orderEventsExchange).with("order.created");
    }

    @Bean
    public TopicExchange productEventsExchange() {
        return new TopicExchange(PRODUCT_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue inventoryProductQueue() {
        return new Queue(INVENTORY_PRODUCT_QUEUE, true);
    }

    @Bean
    public Binding productBinding(@Qualifier("inventoryProductQueue") Queue inventoryProductQueue,
                                  @Qualifier("productEventsExchange") TopicExchange productEventsExchange) {
        return BindingBuilder.bind(inventoryProductQueue).to(productEventsExchange).with("product.created");
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
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                                     @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(INVENTORY_DEAD_ROUTING_KEY);
    }
}
