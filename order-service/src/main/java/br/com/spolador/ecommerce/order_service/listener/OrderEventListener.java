package br.com.spolador.ecommerce.order_service.listener;

import br.com.spolador.ecommerce.order_service.event.OrderCancelledEvent;
import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import br.com.spolador.ecommerce.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = "order-confirmed-queue")
    public void handleOrderConfirmed(OrderCreatedEvent event) {
        orderService.updateOrderStatus(event.orderNumber(), OrderStatus.CONFIRMED);

    }

    @RabbitListener(queues = "order-cancelled-queue")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        orderService.updateOrderStatus(event.orderNumber(), OrderStatus.CANCELLED);

    }
}
