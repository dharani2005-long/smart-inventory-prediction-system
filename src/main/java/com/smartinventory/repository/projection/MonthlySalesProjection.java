package com.smartinventory.repository.projection;

import java.math.BigDecimal;

/** Aggregated sales for a single (year, month). */
public interface MonthlySalesProjection {
    Integer getYear();
    Integer getMonth();
    Long getTotalQuantity();
    BigDecimal getTotalAmount();
}
