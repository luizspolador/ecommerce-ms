package br.com.spolador.ecommerce.notification_service.event;

import br.com.spolador.ecommerce.notification_service.factory.NotificationFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderConfirmedEvent Unit Tests")
class OrderConfirmedEventTest {

    @Nested
    @DisplayName("Creation and Accessors")
    class CreationAndAccessors {

        @Test
        @DisplayName("Should create event using constructor and read accessors")
        void givenValues_whenConstructed_thenFieldsMatch() {
            OrderConfirmedEvent event = new OrderConfirmedEvent("ORD-100", "test@example.com");

            assertThat(event.orderNumber()).isEqualTo("ORD-100");
            assertThat(event.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should create event using builder")
        void givenBuilder_whenBuilt_thenFieldsMatch() {
            OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                    .orderNumber("ORD-200")
                    .email("builder@example.com")
                    .build();

            assertThat(event.orderNumber()).isEqualTo("ORD-200");
            assertThat(event.email()).isEqualTo("builder@example.com");
            assertThat(OrderConfirmedEvent.builder().toString()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Equals, HashCode and ToString")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Should verify equality and hashCode contract")
        void givenTwoEqualEvents_whenCompared_thenShouldBeEqual() {
            OrderConfirmedEvent event1 = NotificationFactory.createOrderConfirmedEvent();
            OrderConfirmedEvent event2 = NotificationFactory.createOrderConfirmedEvent();
            OrderConfirmedEvent different = new OrderConfirmedEvent("DIFF-999", "diff@example.com");

            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
            assertThat(event1).isNotEqualTo(different);
            assertThat(event1).isNotEqualTo(null);
            assertThat(event1).isNotEqualTo(new Object());
            assertThat(event1.toString()).contains("ORD-12345", "customer@example.com");
        }
    }
}
