package br.com.spolador.ecommerce.inventory_service.repository;

import br.com.spolador.ecommerce.inventory_service.model.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, Long> {
    boolean existsByOrderNumber(String orderNumber);
}
