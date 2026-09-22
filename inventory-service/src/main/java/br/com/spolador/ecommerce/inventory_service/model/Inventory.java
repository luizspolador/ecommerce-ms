package br.com.spolador.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_inventory", indexes = {
        @Index(name = "idx_inventory_sku", columnList = "sku", unique = true)
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;
}
