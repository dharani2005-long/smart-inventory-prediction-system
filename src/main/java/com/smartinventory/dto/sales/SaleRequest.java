package com.smartinventory.dto.sales;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SaleRequest(

        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        /** Optional. Defaults to today when null. */
        LocalDate saleDate,

        @Size(max = 60)
        String invoiceNo
) {}
