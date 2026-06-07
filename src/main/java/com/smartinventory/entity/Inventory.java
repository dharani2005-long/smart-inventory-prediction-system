package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Live stock level for a single product (1:1 with {@link Product}).
 * Uses optimistic locking ({@code @Version}) to guard concurrent stock changes.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantityOnHand = 0;

    /** Stock reserved for pending orders (not available for sale). */
    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Version
    private Long version;

    /** Available = on-hand minus reserved. */
    @Transient
    public int getAvailableQuantity() {
        return quantityOnHand - reservedQuantity;
    }
}
