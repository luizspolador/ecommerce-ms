package br.com.spolador.ecommerce.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order-events";
    public static final String ORDER_DLX = "order-dlx";
    public static final String ORDER_DLQ = "order-dlq";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.dead";

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable("order-confirmed-queue")
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding confirmedBinding(Queue orderConfirmedQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderConfirmedQueue)
                .to(orderEventsExchange)
                .with("order.confirmed");
    }

    @Bean
    public Queue orderCancelledQueue() {
        return QueueBuilder.durable("order-cancelled-queue")
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding cancelledBinding(Queue orderCancelledQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderCancelledQueue)
                .to(orderEventsExchange)
                .with("order.cancelled");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(ORDER_DLX);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(ORDER_DLQ, true);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(ORDER_DEAD_ROUTING_KEY);
    }
}
