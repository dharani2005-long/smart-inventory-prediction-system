package com.smartinventory.controller;

import com.smartinventory.service.ExcelReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Reports", description = "Excel report exports")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelReportService excelReportService;

    @Operation(summary = "Export inventory report (Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/inventory")
    public ResponseEntity<byte[]> inventory() {
        return file(excelReportService.inventoryReport(), "inventory-report.xlsx");
    }

    @Operation(summary = "Export sales report (Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/sales")
    public ResponseEntity<byte[]> sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate s = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate e = end != null ? end : LocalDate.now();
        return file(excelReportService.salesReport(s, e), "sales-report.xlsx");
    }

    @Operation(summary = "Export supplier report (Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/suppliers")
    public ResponseEntity<byte[]> suppliers() {
        return file(excelReportService.supplierReport(), "supplier-report.xlsx");
    }

    @Operation(summary = "Export forecast report (Excel)")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/forecast")
    public ResponseEntity<byte[]> forecast() {
        return file(excelReportService.forecastReport(), "forecast-report.xlsx");
    }

    private ResponseEntity<byte[]> file(byte[] body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(XLSX))
                .body(body);
    }
}
