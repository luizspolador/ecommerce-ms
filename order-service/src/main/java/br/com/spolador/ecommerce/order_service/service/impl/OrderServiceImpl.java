package br.com.spolador.ecommerce.order_service.service.impl;

import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.event.OrderCreatedEvent;
import br.com.spolador.ecommerce.order_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.order_service.mapper.OrderMapper;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.model.OrderLineItems;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;
import br.com.spolador.ecommerce.order_service.repository.OrderRepository;
import br.com.spolador.ecommerce.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${order.enabled:true}")
    private boolean orderEnabled;

    public OrderResponseDTO fallbackMethod(OrderRequestDTO orderRequest, String userId, Throwable throwable) {
        log.error("Circuit breaker activated. Cause: {}", throwable.getMessage());
        throw new RuntimeException("The inventory service is not responding, please try again later.");

    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequest, String userId) {
       if(!orderEnabled) {
           log.warn("Order denied. Service disabled by configuration");
           throw new RuntimeException("The ordering service is under maintenance. Try again in a few minutes");
       }
       log.info(("Inserting a new order"));
       Order order = orderMapper.toOrder(orderRequest);
       order.setUserId(userId);
       order.setOrderNumber(UUID.randomUUID().toString());
       order.setOrderStatus(OrderStatus.CREATED);
       Order createdOrder = orderRepository.save(order);
       log.info("Order created with ID: {}", createdOrder.getId());
       List<OrderCreatedEvent.OrderItemEvent> orderItems = order.getOrderLineItemList()
               .stream().map((OrderLineItems item) -> new OrderCreatedEvent.OrderItemEvent(
                       item.getSku(), item.getPrice().toString(), item.getQuantity()
               )).toList();
       OrderCreatedEvent event = new OrderCreatedEvent(
               createdOrder.getOrderNumber(), orderRequest.getEmail(), orderItems
       );
       rabbitTemplate.convertAndSend("order-events", "order.created", event);
       log.info("Event submitted to RabbitMQ with order: {}", createdOrder.getOrderNumber());
       return orderMapper.toOrderResponse(createdOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrders(String userId, boolean isAdmin) {
        List<Order> orders;
        if(isAdmin) {
            orders = orderRepository.findAll();
        } else {
            orders = orderRepository.findByUserId(userId);
        }
        return orders.stream()
                .map(orderMapper::toOrderResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Order", "id", id)
                );
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        if(!orderRepository.existsById(id)){
            throw new ResourceNotFoundException("Order", "id", id);
        }
        orderRepository.deleteById(id);
        log.info("Order with id {} was deleted", id);
    }

    @Override
    @Transactional
    public void updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        log.info("Updating database, order: {} -> {}", orderNumber, newStatus);
        orderRepository.findByOrderNumber(orderNumber).ifPresentOrElse(
                order -> {
                    order.setOrderStatus(newStatus);
                    orderRepository.save(order);
                    log.info("Order state updated for order: {}", orderNumber);
                },
                () -> log.error("The order {} was not found for updating.", orderNumber)
        );
    }
}
