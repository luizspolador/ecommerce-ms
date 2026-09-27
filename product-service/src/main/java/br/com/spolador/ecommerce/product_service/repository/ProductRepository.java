package br.com.spolador.ecommerce.product_service.repository;

import br.com.spolador.ecommerce.product_service.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);
}

