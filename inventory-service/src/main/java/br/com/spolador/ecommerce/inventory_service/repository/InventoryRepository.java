package br.com.spolador.ecommerce.inventory_service.repository;

import br.com.spolador.ecommerce.inventory_service.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findBySku(String sku);
    boolean existsBySku(String sku);
}
