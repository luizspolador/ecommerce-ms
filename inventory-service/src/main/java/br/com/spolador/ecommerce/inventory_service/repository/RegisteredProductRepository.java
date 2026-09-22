package br.com.spolador.ecommerce.inventory_service.repository;

import br.com.spolador.ecommerce.inventory_service.model.RegisteredProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredProductRepository extends JpaRepository<RegisteredProduct, Long> {
    Optional<RegisteredProduct> findBySku(String sku);
    boolean existsBySku(String sku);
    void deleteBySku(String sku);
}
