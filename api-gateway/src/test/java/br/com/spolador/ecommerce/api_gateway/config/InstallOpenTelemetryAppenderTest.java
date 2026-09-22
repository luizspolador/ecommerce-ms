package br.com.spolador.ecommerce.api_gateway.config;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Unit tests for InstallOpenTelemetryAppender in api-gateway")
class InstallOpenTelemetryAppenderTest {

    @Test
    @DisplayName("Should install OpenTelemetry appender without errors")
    void testAfterPropertiesSet() {
        OpenTelemetry openTelemetry = OpenTelemetry.noop();
        InstallOpenTelemetryAppender appender = new InstallOpenTelemetryAppender(openTelemetry);

        assertThatCode(appender::afterPropertiesSet).doesNotThrowAnyException();
    }
}
