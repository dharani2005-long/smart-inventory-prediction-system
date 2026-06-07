package com.smartinventory.entity;

import com.smartinventory.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Immutable record of a single stock movement. Provides full transaction history
 * and is the source for stock-level reconciliation.
 */
@Entity
@Table(name = "stock_transactions", indexes = {
        @Index(name = "idx_txn_product", columnList = "product_id"),
        @Index(name = "idx_txn_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransaction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /** Signed quantity applied to stock (already includes direction). */
    @Column(nullable = false)
    private Integer quantity;

    /** Stock level immediately after this transaction (for auditing). */
    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(length = 255)
    private String note;

    /** Reference document number (PO, invoice, return slip). */
    @Column(length = 60)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
}
