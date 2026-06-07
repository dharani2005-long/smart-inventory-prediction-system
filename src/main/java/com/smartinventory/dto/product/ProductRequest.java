package com.smartinventory.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description,

        @NotBlank(message = "SKU is required")
        @Size(max = 60)
        String sku,

        @Size(max = 60)
        String barcode,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be >= 0")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.0", inclusive = true, message = "Cost price must be >= 0")
        BigDecimal costPrice,

        @NotNull(message = "Reorder level is required")
        @Min(value = 0, message = "Reorder level must be >= 0")
        Integer reorderLevel,

        Long categoryId,

        Long supplierId,

        /** Optional opening stock when creating the product. */
        @Min(value = 0, message = "Opening stock must be >= 0")
        Integer openingStock
) {}
