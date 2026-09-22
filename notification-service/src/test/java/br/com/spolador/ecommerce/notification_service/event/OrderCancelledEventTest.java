package br.com.spolador.ecommerce.notification_service.event;

import br.com.spolador.ecommerce.notification_service.factory.NotificationFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderCancelledEvent Unit Tests")
class OrderCancelledEventTest {

    @Nested
    @DisplayName("Creation and Accessors")
    class CreationAndAccessors {

        @Test
        @DisplayName("Should create event using constructor and read accessors")
        void givenValues_whenConstructed_thenFieldsMatch() {
            OrderCancelledEvent event = new OrderCancelledEvent("ORD-100", "test@example.com", "Item damaged");

            assertThat(event.orderNumber()).isEqualTo("ORD-100");
            assertThat(event.email()).isEqualTo("test@example.com");
            assertThat(event.reason()).isEqualTo("Item damaged");
        }
    }

    @Nested
    @DisplayName("Equals, HashCode and ToString")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Should verify equality and hashCode contract")
        void givenTwoEqualEvents_whenCompared_thenShouldBeEqual() {
            OrderCancelledEvent event1 = NotificationFactory.createOrderCancelledEvent();
            OrderCancelledEvent event2 = NotificationFactory.createOrderCancelledEvent();
            OrderCancelledEvent different = new OrderCancelledEvent("DIFF-999", "diff@example.com", "Different reason");

            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
            assertThat(event1).isNotEqualTo(different);
            assertThat(event1).isNotEqualTo(null);
            assertThat(event1).isNotEqualTo(new Object());
            assertThat(event1.toString()).contains("ORD-12345", "customer@example.com", "Insufficient stock");
        }
    }
}
