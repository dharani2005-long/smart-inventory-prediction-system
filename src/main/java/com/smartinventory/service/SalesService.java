package com.smartinventory.service;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.sales.*;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.Sale;
import com.smartinventory.entity.User;
import com.smartinventory.repository.SaleRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesService {

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final StockTransactionService stockTransactionService;
    private final UserRepository userRepository;

    @Transactional
    public SaleResponse record(SaleRequest req) {
        Product product = productService.getEntity(req.productId());
        LocalDate date = req.saleDate() != null ? req.saleDate() : LocalDate.now();

        // Decrement stock and log a STOCK_OUT (throws if insufficient stock).
        stockTransactionService.recordSaleStockOut(product, req.quantity(), req.invoiceNo());

        BigDecimal unitPrice = product.getUnitPrice();
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(req.quantity()));

        Sale sale = Sale.builder()
                .product(product)
                .quantity(req.quantity())
                .unitPrice(unitPrice)
                .totalAmount(total)
                .saleDate(date)
                .invoiceNo(req.invoiceNo())
                .recordedBy(currentUser())
                .build();
        sale = saleRepository.save(sale);
        log.info("Recorded sale of {} x '{}' = {}", req.quantity(), product.getName(), total);
        return SaleResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> listByDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        return PageResponse.from(saleRepository.findBySaleDateBetween(start, end, pageable), SaleResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> listByProduct(Long productId, Pageable pageable) {
        productService.getEntity(productId);
        return PageResponse.from(saleRepository.findByProductId(productId, pageable), SaleResponse::from);
    }

    /** Daily sales report between two dates (inclusive). */
    @Transactional(readOnly = true)
    public List<DailySalesReport> dailyReport(LocalDate start, LocalDate end) {
        return saleRepository.dailySalesReport(start, end).stream()
                .map(p -> new DailySalesReport(p.getSaleDate(), nz(p.getTotalQuantity()),
                        p.getTotalAmount(), nz(p.getTransactionCount())))
                .toList();
    }

    /** Monthly sales report for a given year. */
    @Transactional(readOnly = true)
    public List<MonthlySalesReport> monthlyReport(int year) {
        return saleRepository.monthlySalesReport(year).stream()
                .map(p -> new MonthlySalesReport(p.getYear(), p.getMonth(),
                        Month.of(p.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        nz(p.getTotalQuantity()), p.getTotalAmount()))
                .toList();
    }

    /** Product-wise sales analysis between two dates. */
    @Transactional(readOnly = true)
    public List<ProductSalesReport> productWise(LocalDate start, LocalDate end) {
        return saleRepository.productWiseSales(start, end).stream()
                .map(p -> new ProductSalesReport(p.getProductId(), p.getProductName(), p.getSku(),
                        nz(p.getTotalQuantity()), p.getTotalAmount()))
                .toList();
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }

    private User currentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);
    }
}
