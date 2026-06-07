package com.smartinventory.dto.sales;

import com.smartinventory.entity.Sale;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record SaleResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        LocalDate saleDate,
        String invoiceNo,
        String recordedBy
) {
    public static SaleResponse from(Sale s) {
        return SaleResponse.builder()
                .id(s.getId())
                .productId(s.getProduct().getId())
                .productName(s.getProduct().getName())
                .quantity(s.getQuantity())
                .unitPrice(s.getUnitPrice())
                .totalAmount(s.getTotalAmount())
                .saleDate(s.getSaleDate())
                .invoiceNo(s.getInvoiceNo())
                .recordedBy(s.getRecordedBy() != null ? s.getRecordedBy().getUsername() : null)
                .build();
    }
}
