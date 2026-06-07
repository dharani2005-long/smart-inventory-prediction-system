package com.smartinventory.repository.projection;

import java.math.BigDecimal;

/** Aggregated sales grouped by product (product-wise analysis). */
public interface ProductSalesProjection {
    Long getProductId();
    String getProductName();
    String getSku();
    Long getTotalQuantity();
    BigDecimal getTotalAmount();
}
