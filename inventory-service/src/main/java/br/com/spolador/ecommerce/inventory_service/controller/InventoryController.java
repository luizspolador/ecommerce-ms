package br.com.spolador.ecommerce.inventory_service.controller;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;
import br.com.spolador.ecommerce.inventory_service.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(@PathVariable String sku, @RequestParam Integer quantity) {
        return inventoryService.isInStock(sku, quantity);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponseDTO createInventory(@Valid @RequestBody InventoryRequestDTO inventoryRequest) {
        return inventoryService.createInventory(inventoryRequest);
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponseDTO> getAllInventories(HttpServletRequest request){
        System.out.println("Request from port: " + request.getServerPort());
        return inventoryService.getAllInventories();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponseDTO updateInventory(@PathVariable Long id,
                                                @Valid @RequestBody InventoryRequestDTO inventoryRequest) {
        return inventoryService.updateInventoryById(id, inventoryRequest);
    }

    @PutMapping("/reduce/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public String reduceStock(@PathVariable String sku,
                                                @RequestParam Integer quantity) {
        inventoryService.reduceStock(sku, quantity);
        return "Stock was reduced";
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventoryById(id);
    }
}
