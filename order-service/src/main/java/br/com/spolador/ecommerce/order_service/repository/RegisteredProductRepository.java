package br.com.spolador.ecommerce.order_service.repository;

import br.com.spolador.ecommerce.order_service.model.RegisteredProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredProductRepository extends JpaRepository<RegisteredProduct, Long> {
    boolean existsBySku(String sku);
    Optional<RegisteredProduct> findBySku(String sku);
}
