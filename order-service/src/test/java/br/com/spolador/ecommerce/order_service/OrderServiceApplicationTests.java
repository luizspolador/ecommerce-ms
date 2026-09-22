package br.com.spolador.ecommerce.order_service;

import br.com.spolador.ecommerce.order_service.config.InstallOpenTelemetryAppender;
import br.com.spolador.ecommerce.order_service.repository.OrderRepository;
import br.com.spolador.ecommerce.order_service.repository.OutboxRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OrderServiceApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OutboxRepository outboxRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private OpenTelemetry openTelemetry;

    @MockitoBean
    private Tracer tracer;

    @MockitoBean
    private InstallOpenTelemetryAppender installOpenTelemetryAppender;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        assertNotNull(orderRepository);
        assertNotNull(outboxRepository);
    }

}
