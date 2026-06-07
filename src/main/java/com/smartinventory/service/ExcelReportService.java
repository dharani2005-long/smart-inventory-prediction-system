package com.smartinventory.service;

import com.smartinventory.dto.forecast.ForecastResponse;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.Sale;
import com.smartinventory.entity.Supplier;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleRepository;
import com.smartinventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Generates downloadable Excel (.xlsx) reports via Apache POI. */
@Service
@RequiredArgsConstructor
public class ExcelReportService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final SaleRepository saleRepository;
    private final PredictionService predictionService;

    @Transactional(readOnly = true)
    public byte[] inventoryReport() {
        List<Product> products = productRepository.findAll();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventory");
            writeHeader(wb, sheet, "SKU", "Name", "Category", "Supplier",
                    "Unit Price", "Reorder Level", "On Hand", "Stock Value", "Status");
            int r = 1;
            for (Product p : products) {
                int stock = p.getInventory() != null && p.getInventory().getQuantityOnHand() != null
                        ? p.getInventory().getQuantityOnHand() : 0;
                double value = stock * (p.getUnitPrice() != null ? p.getUnitPrice().doubleValue() : 0);
                Row row = sheet.createRow(r++);
                cell(row, 0, p.getSku());
                cell(row, 1, p.getName());
                cell(row, 2, p.getCategory() != null ? p.getCategory().getName() : "-");
                cell(row, 3, p.getSupplier() != null ? p.getSupplier().getName() : "-");
                cell(row, 4, p.getUnitPrice() != null ? p.getUnitPrice().doubleValue() : 0);
                cell(row, 5, p.getReorderLevel());
                cell(row, 6, stock);
                cell(row, 7, value);
                cell(row, 8, stock <= p.getReorderLevel() ? "LOW STOCK" : "OK");
            }
            autoSize(sheet, 9);
            return toBytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Failed to generate inventory report");
        }
    }

    @Transactional(readOnly = true)
    public byte[] salesReport(LocalDate start, LocalDate end) {
        List<Sale> sales = saleRepository.findBySaleDateBetween(start, end,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sales");
            writeHeader(wb, sheet, "Date", "Invoice", "Product", "Qty", "Unit Price", "Total", "Recorded By");
            int r = 1;
            for (Sale s : sales) {
                Row row = sheet.createRow(r++);
                cell(row, 0, s.getSaleDate().toString());
                cell(row, 1, s.getInvoiceNo() != null ? s.getInvoiceNo() : "-");
                cell(row, 2, s.getProduct().getName());
                cell(row, 3, s.getQuantity());
                cell(row, 4, s.getUnitPrice().doubleValue());
                cell(row, 5, s.getTotalAmount().doubleValue());
                cell(row, 6, s.getRecordedBy() != null ? s.getRecordedBy().getUsername() : "-");
            }
            autoSize(sheet, 7);
            return toBytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Failed to generate sales report");
        }
    }

    @Transactional(readOnly = true)
    public byte[] supplierReport() {
        List<Supplier> suppliers = supplierRepository.findAll();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Suppliers");
            writeHeader(wb, sheet, "Name", "Contact Person", "Email", "Phone", "Address",
                    "Linked Products", "Active");
            int r = 1;
            for (Supplier s : suppliers) {
                long productCount = productRepository.findBySupplierId(s.getId()).size();
                Row row = sheet.createRow(r++);
                cell(row, 0, s.getName());
                cell(row, 1, s.getContactPerson() != null ? s.getContactPerson() : "-");
                cell(row, 2, s.getEmail() != null ? s.getEmail() : "-");
                cell(row, 3, s.getPhone() != null ? s.getPhone() : "-");
                cell(row, 4, s.getAddress() != null ? s.getAddress() : "-");
                cell(row, 5, productCount);
                cell(row, 6, s.isActive() ? "Yes" : "No");
            }
            autoSize(sheet, 7);
            return toBytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Failed to generate supplier report");
        }
    }

    @Transactional
    public byte[] forecastReport() {
        List<ForecastResponse> forecasts = predictionService.latestForAll();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Forecast");
            writeHeader(wb, sheet, "SKU", "Product", "Avg Daily Use", "Horizon (days)",
                    "Forecast Demand", "Current Stock", "Depletion Date",
                    "Reorder Qty", "Low Stock", "Confidence %", "Recommendation");
            int r = 1;
            for (ForecastResponse f : forecasts) {
                Row row = sheet.createRow(r++);
                cell(row, 0, f.sku());
                cell(row, 1, f.productName());
                cell(row, 2, f.avgDailyConsumption());
                cell(row, 3, f.forecastHorizonDays());
                cell(row, 4, f.forecastDemand());
                cell(row, 5, f.currentStock());
                cell(row, 6, f.depletionExpectedOn() != null ? f.depletionExpectedOn().toString() : "-");
                cell(row, 7, f.recommendedReorderQty());
                cell(row, 8, f.lowStock() ? "YES" : "no");
                cell(row, 9, f.confidencePercent());
                cell(row, 10, f.recommendation());
            }
            autoSize(sheet, 11);
            return toBytes(wb);
        } catch (IOException e) {
            throw new BusinessException("Failed to generate forecast report");
        }
    }

    // ---- POI helpers ----

    private void writeHeader(Workbook wb, Sheet sheet, String... titles) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);

        Row header = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(titles[i]);
            c.setCellStyle(style);
        }
    }

    private void cell(Row row, int col, String value) { row.createCell(col).setCellValue(value); }
    private void cell(Row row, int col, double value) { row.createCell(col).setCellValue(value); }
    private void cell(Row row, int col, long value) { row.createCell(col).setCellValue(value); }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }
}
