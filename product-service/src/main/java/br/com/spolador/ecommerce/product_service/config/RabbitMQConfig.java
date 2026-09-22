package br.com.spolador.ecommerce.product_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_EVENTS_EXCHANGE = "product-events";

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange productEventsExchange() {
        return new TopicExchange(PRODUCT_EVENTS_EXCHANGE);
    }
}
