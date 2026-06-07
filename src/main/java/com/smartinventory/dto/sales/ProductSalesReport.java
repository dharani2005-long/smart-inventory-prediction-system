package com.smartinventory.dto.sales;

import java.math.BigDecimal;

public record ProductSalesReport(
        Long productId,
        String productName,
        String sku,
        long totalQuantity,
        BigDecimal totalAmount
) {}
