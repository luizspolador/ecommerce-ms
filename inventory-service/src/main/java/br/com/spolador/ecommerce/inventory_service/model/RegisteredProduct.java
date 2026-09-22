package br.com.spolador.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_registered_product", indexes = {
        @Index(name = "idx_registered_product_sku", columnList = "sku", unique = true)
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class RegisteredProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    private LocalDateTime registeredAt;
}
