package com.smartinventory.dto.stock;

import com.smartinventory.entity.StockTransaction;
import com.smartinventory.enums.TransactionType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StockTransactionResponse(
        Long id,
        Long productId,
        String productName,
        TransactionType type,
        Integer quantity,
        Integer balanceAfter,
        String referenceNo,
        String note,
        Long supplierId,
        String supplierName,
        String performedBy,
        LocalDateTime createdAt
) {
    public static StockTransactionResponse from(StockTransaction t) {
        return StockTransactionResponse.builder()
                .id(t.getId())
                .productId(t.getProduct().getId())
                .productName(t.getProduct().getName())
                .type(t.getType())
                .quantity(t.getQuantity())
                .balanceAfter(t.getBalanceAfter())
                .referenceNo(t.getReferenceNo())
                .note(t.getNote())
                .supplierId(t.getSupplier() != null ? t.getSupplier().getId() : null)
                .supplierName(t.getSupplier() != null ? t.getSupplier().getName() : null)
                .performedBy(t.getPerformedBy() != null ? t.getPerformedBy().getUsername() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
