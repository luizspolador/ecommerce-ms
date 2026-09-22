package br.com.spolador.ecommerce.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class NotificationServiceApplicationTests {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        assertNotNull(javaMailSender);
        assertNotNull(connectionFactory);
        assertNotNull(rabbitTemplate);
    }

}
