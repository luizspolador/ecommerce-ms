package br.com.spolador.ecommerce.product_service.repository;

import br.com.spolador.ecommerce.product_service.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
}
