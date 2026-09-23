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
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "orderStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toOrder(final OrderRequestDTO orderRequest);

    @Mapping(target = "id", ignore = true)
    OrderLineItems toOrderLineItems(final OrderLineItemRequestDTO orderLineItemsRequest);

    OrderResponseDTO toOrderResponse(final Order order);

    OrderLineItemResponseDTO toOrderLineItemsResponse(final OrderLineItems orderLineItems);
}
