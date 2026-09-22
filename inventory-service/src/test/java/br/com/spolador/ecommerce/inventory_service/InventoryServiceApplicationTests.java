package br.com.spolador.ecommerce.inventory_service;

import br.com.spolador.ecommerce.inventory_service.config.InstallOpenTelemetryAppender;
import br.com.spolador.ecommerce.inventory_service.repository.InventoryRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class InventoryServiceApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private InventoryRepository inventoryRepository;

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
    private br.com.spolador.ecommerce.inventory_service.repository.ProcessedOrderRepository processedOrderRepository;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
        assertNotNull(inventoryRepository);
        assertNotNull(rabbitTemplate);
    }

}
