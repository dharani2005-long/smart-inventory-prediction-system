package com.smartinventory.dto.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesReport(
        LocalDate date,
        long totalQuantity,
        BigDecimal totalAmount,
        long transactionCount
) {}
