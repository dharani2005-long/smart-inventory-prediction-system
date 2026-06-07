package com.smartinventory.controller;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.sales.*;
import com.smartinventory.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Sales", description = "Record sales and view sales reports")
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @Operation(summary = "Record a sale (decrements stock)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @PostMapping
    public ResponseEntity<SaleResponse> record(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesService.record(request));
    }

    @Operation(summary = "List sales in a date range (paged)")
    @GetMapping
    public ResponseEntity<PageResponse<SaleResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 15, sort = "saleDate") Pageable pageable) {
        LocalDate s = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate e = end != null ? end : LocalDate.now();
        return ResponseEntity.ok(salesService.listByDateRange(s, e, pageable));
    }

    @Operation(summary = "Daily sales report")
    @GetMapping("/reports/daily")
    public ResponseEntity<List<DailySalesReport>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate e = end != null ? end : LocalDate.now();
        return ResponseEntity.ok(salesService.dailyReport(s, e));
    }

    @Operation(summary = "Monthly sales report for a year")
    @GetMapping("/reports/monthly")
    public ResponseEntity<List<MonthlySalesReport>> monthly(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(salesService.monthlyReport(y));
    }

    @Operation(summary = "Product-wise sales analysis")
    @GetMapping("/reports/product-wise")
    public ResponseEntity<List<ProductSalesReport>> productWise(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate e = end != null ? end : LocalDate.now();
        return ResponseEntity.ok(salesService.productWise(s, e));
    }
}
