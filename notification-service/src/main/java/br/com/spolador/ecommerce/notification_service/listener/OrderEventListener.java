package br.com.spolador.ecommerce.notification_service.listener;

import br.com.spolador.ecommerce.notification_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.notification_service.event.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@RabbitListener(queues = "notification-queue")
public class OrderEventListener {

    private final JavaMailSender mailSender;

    @RabbitHandler
    public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
        log.info("Order confirmed to order number: {}", event.orderNumber());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("orders@ecommerce.com");
        message.setTo(event.email());
        message.setSubject("Order confirmed - " + event.orderNumber());
        message.setText("Hi!\n\n" +
                "Your order number " + event.orderNumber() + " has been successfully received. \n" +
                "You will soon receive updates on the shipment.\n\n" +
                "Thank you for shopping with us!");
        mailSender.send(message);
        log.info("Sending confirmation email: {}", event.email());
        log.info("Email successfully sent for the order: {}", event.orderNumber());

    }

    @RabbitHandler
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        log.warn("Sending cancellation email for the order: {}", event.orderNumber());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("orders@ecommerce.com");
        message.setTo(event.email());
        message.setSubject("Order updated - " + event.orderNumber());
        message.setText("Hi!\n\n" +
                "We regret to inform you that your order " + event.orderNumber() + " has been cancelled. \n" +
                "Reason: " + event.reason() + ". \n" +
                "Thank you!");
        mailSender.send(message);
        log.info("Cancellation email successfully sent to: {}", event.email());

    }
}
