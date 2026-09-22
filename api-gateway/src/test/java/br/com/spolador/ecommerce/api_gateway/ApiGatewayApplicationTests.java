package br.com.spolador.ecommerce.api_gateway;

import br.com.spolador.ecommerce.api_gateway.config.InstallOpenTelemetryAppender;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApiGatewayApplicationTests {

    @MockitoBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @MockitoBean
    private InstallOpenTelemetryAppender installOpenTelemetryAppender;

    @MockitoBean
    private OpenTelemetry openTelemetry;

    @MockitoBean
    private Tracer tracer;

    @Test
    void contextLoads() {
        assertNotNull(reactiveJwtDecoder);
    }

}
