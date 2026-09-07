package br.com.spolador.ecommerce.inventory_service.service;

import br.com.spolador.ecommerce.inventory_service.dto.InventoryRequestDTO;
import br.com.spolador.ecommerce.inventory_service.dto.InventoryResponseDTO;

import java.util.List;

public interface InventoryService {
    boolean isInStock (String sku, Integer quantity);
    InventoryResponseDTO createInventory(InventoryRequestDTO inventoryRequestDTO);
    List<InventoryResponseDTO> getAllInventories();
    InventoryResponseDTO updateInventoryById(Long id, InventoryRequestDTO inventoryRequestDTO);
    void deleteInventoryById(Long id);
    void reduceStock(String sku, Integer quantity);
}
