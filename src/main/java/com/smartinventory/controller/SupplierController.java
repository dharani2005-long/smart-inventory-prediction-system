package com.smartinventory.controller;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.product.ProductResponse;
import com.smartinventory.dto.stock.StockTransactionResponse;
import com.smartinventory.dto.supplier.SupplierRequest;
import com.smartinventory.dto.supplier.SupplierResponse;
import com.smartinventory.service.StockTransactionService;
import com.smartinventory.service.SupplierService;
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

import java.util.List;

@Tag(name = "Suppliers", description = "Supplier management")
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final StockTransactionService stockTransactionService;

    @Operation(summary = "Add a supplier")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @Operation(summary = "Update a supplier")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @Operation(summary = "Delete (deactivate) a supplier")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get supplier by id")
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.get(id));
    }

    @Operation(summary = "Search suppliers (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<SupplierResponse>> search(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(supplierService.search(keyword, pageable));
    }

    @Operation(summary = "List products linked to a supplier")
    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductResponse>> products(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.productsOfSupplier(id));
    }

    @Operation(summary = "View supplier stock-transaction history (paged)")
    @GetMapping("/{id}/history")
    public ResponseEntity<PageResponse<StockTransactionResponse>> history(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(stockTransactionService.historyBySupplier(id, pageable));
    }
}
