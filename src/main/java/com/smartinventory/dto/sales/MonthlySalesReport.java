package com.smartinventory.dto.sales;

import java.math.BigDecimal;

public record MonthlySalesReport(
        int year,
        int month,
        String monthName,
        long totalQuantity,
        BigDecimal totalAmount
) {}
