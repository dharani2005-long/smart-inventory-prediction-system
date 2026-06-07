package com.smartinventory.dto.product;

import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.Product;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        String barcode,
        BigDecimal unitPrice,
        BigDecimal costPrice,
        Integer reorderLevel,
        boolean active,
        Long categoryId,
        String categoryName,
        Long supplierId,
        String supplierName,
        Integer currentStock,
        boolean lowStock
) {

    public static ProductResponse from(Product p, Inventory inventory) {
        int stock = inventory != null && inventory.getQuantityOnHand() != null
                ? inventory.getQuantityOnHand() : 0;
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .sku(p.getSku())
                .barcode(p.getBarcode())
                .unitPrice(p.getUnitPrice())
                .costPrice(p.getCostPrice())
                .reorderLevel(p.getReorderLevel())
                .active(p.isActive())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .supplierId(p.getSupplier() != null ? p.getSupplier().getId() : null)
                .supplierName(p.getSupplier() != null ? p.getSupplier().getName() : null)
                .currentStock(stock)
                .lowStock(stock <= p.getReorderLevel())
                .build();
    }
}
