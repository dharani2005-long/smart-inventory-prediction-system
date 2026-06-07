package com.smartinventory.dto.stock;

import com.smartinventory.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockTransactionRequest(

        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        /**
         * Quantity. For STOCK_IN / STOCK_OUT / RETURN provide a positive number.
         * For ADJUSTMENT provide a signed number (+/-) representing the delta.
         */
        @NotNull(message = "Quantity is required")
        Integer quantity,

        Long supplierId,

        @Size(max = 60)
        String referenceNo,

        @Size(max = 255)
        String note
) {}
