package br.com.spolador.ecommerce.order_service.service.impl;

import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.exception.ResourceNotFoundException;
import br.com.spolador.ecommerce.order_service.mapper.OrderMapper;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.repository.OrderRepository;
import br.com.spolador.ecommerce.order_service.service.OrderService;
import br.com.spolador.ecommerce.order_service.service.client.InventoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final InventoryClient inventoryClient;

    @Value("${order.enabled:true}")
    private boolean orderEnabled;

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
        for(var item : order.getOrderLineItemList()) {
            String sku = item.getSku();
            Integer quantity = item.getQuantity();
            try {
                inventoryClient.reduceStock(sku, quantity);
            } catch(Exception ex) {
                log.error("Error to reduce stock for product {}: {}", sku, ex.getMessage());
                throw new IllegalArgumentException("The order could not be processed: insufficient stock or inventory error");
            }

        }
        order.setOrderNumber(UUID.randomUUID().toString());
        Order createdOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", createdOrder.getId());
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
}
