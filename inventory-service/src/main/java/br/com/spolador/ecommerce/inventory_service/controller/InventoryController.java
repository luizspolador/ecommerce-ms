package br.com.spolador.ecommerce.inventory_service.controller;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory", description = "Endpoints for managing stock levels and inventory items")
public class InventoryController {
    private final InventoryService inventoryService;

    @Operation(summary = "Check if product SKU is in stock", description = "Returns true if the specified SKU has available stock >= requested quantity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock check evaluated successfully")
    })
    @GetMapping("/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(final @PathVariable String sku, final @RequestParam Integer quantity) {
        return inventoryService.isInStock(sku, quantity);
    }

    @Operation(summary = "Create a new inventory record", description = "Registers a new product SKU with initial quantity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inventory record created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "409", description = "Inventory with this SKU already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponseDTO createInventory(final @Valid @RequestBody InventoryRequestDTO inventoryRequest) {
        return inventoryService.createInventory(inventoryRequest);
    }

    @Operation(summary = "List all inventory records", description = "Retrieves all products registered in inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of inventories retrieved successfully")
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponseDTO> getAllInventories(final HttpServletRequest request){
        log.debug("Request from port: {}", request.getServerPort());
        return inventoryService.getAllInventories();
    }

    @Operation(summary = "Update inventory by ID", description = "Updates SKU and quantity for an existing inventory record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory record updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponseDTO updateInventory(final @PathVariable Long id,
                                                final @Valid @RequestBody InventoryRequestDTO inventoryRequest) {
        return inventoryService.updateInventoryById(id, inventoryRequest);
    }

    @Operation(summary = "Reduce stock for a SKU", description = "Deducts the specified quantity from stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock deducted successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock available"),
            @ApiResponse(responseCode = "404", description = "SKU not found in inventory")
    })
    @PutMapping("/reduce/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public String reduceStock(final @PathVariable String sku, final @RequestParam Integer quantity) {
        inventoryService.reduceStock(sku, quantity);
        return "Stock was reduced";
    }

    @Operation(summary = "Delete inventory record by ID", description = "Removes the inventory record from database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Inventory record deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(final @PathVariable Long id) {
        inventoryService.deleteInventoryById(id);
    }
}
