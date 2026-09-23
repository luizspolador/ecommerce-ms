package br.com.spolador.ecommerce.order_service.controller;

import br.com.spolador.ecommerce.order_service.dto.OrderRequestDTO;
import br.com.spolador.ecommerce.order_service.dto.OrderResponseDTO;
import br.com.spolador.ecommerce.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for order creation, tracking and fulfillment")
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "Create a new order", description = "Places an order and triggers inventory check via outbox events")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order payload"),
            @ApiResponse(responseCode = "503", description = "Order service is under maintenance")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDTO createOrder(final @Valid @RequestBody OrderRequestDTO orderRequest,
                                        final @AuthenticationPrincipal Jwt jwt) {
        return orderService.createOrder(orderRequest, jwt.getSubject());
    }

    @Operation(summary = "List orders", description = "Returns customer orders, or all orders if caller has ADMIN role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponseDTO> getOrders(final @AuthenticationPrincipal Jwt jwt) {
        final String userId = jwt.getSubject();
        final boolean isAdmin = hasAdminRole(jwt);
        return orderService.getOrders(userId, isAdmin);
    }

    @Operation(summary = "Get order by ID", description = "Retrieves details of a specific order by its numeric identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view this order"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponseDTO getOrderById(final @PathVariable Long id, final @AuthenticationPrincipal Jwt jwt) {
        final String userId = jwt.getSubject();
        final boolean isAdmin = hasAdminRole(jwt);
        return orderService.getOrderById(id, userId, isAdmin);
    }

    @Operation(summary = "Delete order by ID", description = "Cancels/removes an order by its numeric identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(final @PathVariable Long id) {
        orderService.deleteOrder(id);
    }

    private boolean hasAdminRole(final Jwt jwt) {
        final Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            final List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream().anyMatch(role -> role.equalsIgnoreCase("ADMIN"));
        }
        return false;
    }
}
