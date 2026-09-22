package br.com.spolador.ecommerce.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_processed_order", indexes = {
        @Index(name = "idx_processed_order_number", columnList = "order_number", unique = true)
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessedOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
