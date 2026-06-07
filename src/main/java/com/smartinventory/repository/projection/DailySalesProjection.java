package com.smartinventory.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Aggregated sales for a single day. */
public interface DailySalesProjection {
    LocalDate getSaleDate();
    Long getTotalQuantity();
    BigDecimal getTotalAmount();
    Long getTransactionCount();
}
