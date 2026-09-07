package br.com.spolador.ecommerce.order_service.mapper;

import br.com.spolador.ecommerce.order_service.dto.OrderLineItemRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderLineItemResponseDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.model.Order;
import br.com.spolador.ecommerce.order_service.model.OrderLineItems;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    Order toOrder(OrderRequestDTO orderRequest);

    @Mapping(target = "id", ignore = true)
    OrderLineItems toOrderLineItems(OrderLineItemRequestDTO orderLineItemsRequest);

    OrderResponseDTO toOrderResponse(Order order);

    OrderLineItemResponseDTO toOrderLineItemsResponse(OrderLineItems orderLineItems);
}
