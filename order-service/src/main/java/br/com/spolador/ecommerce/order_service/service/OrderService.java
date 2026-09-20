package br.com.spolador.ecommerce.order_service.service;

import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.model.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponseDTO createOrder(OrderRequestDTO orderRequest, String userId);
    List<OrderResponseDTO> getOrders(String userId, boolean isAdmin);
    OrderResponseDTO getOrderById(Long id);
    void deleteOrder(Long id);
    void updateOrderStatus(String orderNumber, OrderStatus newStatus);
}
