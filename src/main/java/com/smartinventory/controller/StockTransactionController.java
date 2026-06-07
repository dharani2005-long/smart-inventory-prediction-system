package com.smartinventory.controller;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.stock.StockTransactionRequest;
import com.smartinventory.dto.stock.StockTransactionResponse;
import com.smartinventory.service.StockTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Stock Transactions", description = "Stock in / out / return / adjustment and history")
@RestController
@RequestMapping("/api/stock-transactions")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    @Operation(summary = "Record a stock movement (STOCK_IN / STOCK_OUT / RETURN / ADJUSTMENT)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping
    public ResponseEntity<StockTransactionResponse> record(@Valid @RequestBody StockTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockTransactionService.record(request));
    }

    @Operation(summary = "List all transactions (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<StockTransactionResponse>> all(
            @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(stockTransactionService.all(pageable));
    }

    @Operation(summary = "Transaction history for a product (paged)")
    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<StockTransactionResponse>> byProduct(
            @PathVariable Long productId,
            @PageableDefault(size = 15, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(stockTransactionService.historyByProduct(productId, pageable));
    }
}
