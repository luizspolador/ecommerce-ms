package br.com.spolador.ecommerce.notification_service.listener;

import br.com.spolador.ecommerce.notification_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.notification_service.event.OrderConfirmedEvent;
import br.com.spolador.ecommerce.notification_service.factory.NotificationFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventListener Unit Tests")
class OrderEventListenerTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Nested
    @DisplayName("Handle Order Confirmed Event")
    class HandleOrderConfirmedEventTests {

        @Test
        @DisplayName("Should send confirmation email with correct order details")
        void givenOrderConfirmedEvent_whenHandleOrderConfirmedEvent_thenSendsEmail() {
            // Arrange
            OrderConfirmedEvent event = NotificationFactory.createOrderConfirmedEvent();

            // Act
            orderEventListener.handleOrderConfirmedEvent(event);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getFrom()).isEqualTo("orders@ecommerce.com");
            assertThat(Objects.requireNonNull(sentMessage.getTo())[0]).isEqualTo("customer@example.com");
            assertThat(sentMessage.getSubject()).isEqualTo("Order confirmed - " + event.orderNumber());
            assertThat(sentMessage.getText())
                    .contains("Your order number " + event.orderNumber() + " has been successfully received.")
                    .contains("Thank you for shopping with us!");
        }
    }

    @Nested
    @DisplayName("Handle Order Cancelled Event")
    class HandleOrderCancelledEventTests {

        @Test
        @DisplayName("Should send cancellation email with reason and order details")
        void givenOrderCancelledEvent_whenHandleOrderCancelledEvent_thenSendsEmail() {
            // Arrange
            OrderCancelledEvent event = NotificationFactory.createOrderCancelledEvent("Product out of stock");

            // Act
            orderEventListener.handleOrderCancelledEvent(event);

            // Assert
            verify(mailSender).send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getFrom()).isEqualTo("orders@ecommerce.com");
            assertThat(Objects.requireNonNull(sentMessage.getTo())[0]).isEqualTo("customer@example.com");
            assertThat(sentMessage.getSubject()).isEqualTo("Order updated - " + event.orderNumber());
            assertThat(sentMessage.getText())
                    .contains("We regret to inform you that your order " + event.orderNumber() + " has been cancelled.")
                    .contains("Reason: Product out of stock.")
                    .contains("Thank you!");
        }
    }
}
