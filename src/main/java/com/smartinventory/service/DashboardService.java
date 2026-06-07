package com.smartinventory.service;

import com.smartinventory.dto.dashboard.DashboardResponse;
import com.smartinventory.dto.dashboard.DashboardResponse.ChartPoint;
import com.smartinventory.dto.dashboard.DashboardResponse.PredictionSummary;
import com.smartinventory.dto.forecast.ForecastResponse;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleRepository;
import com.smartinventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_DAYS = 14;
    private static final int TOP_PRODUCTS = 5;

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final PredictionService predictionService;

    @Transactional
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long totalProducts = productRepository.countByActiveTrue();
        long totalSuppliers = supplierRepository.countByActiveTrue();
        long lowStock = inventoryRepository.countLowStock();
        BigDecimal monthlySales = saleRepository.totalRevenue(monthStart, today);
        BigDecimal inventoryValue = inventoryRepository.totalInventoryValue();

        // Prediction summary from latest forecasts.
        List<ForecastResponse> forecasts = predictionService.latestForAll();
        long needingReorder = forecasts.stream().filter(f -> f.recommendedReorderQty() > 0).count();
        long predictedLow = forecasts.stream().filter(ForecastResponse::lowStock).count();
        double avgConfidence = forecasts.stream()
                .mapToDouble(ForecastResponse::confidencePercent).average().orElse(0.0);

        PredictionSummary summary = PredictionSummary.builder()
                .productsNeedingReorder(needingReorder)
                .productsPredictedLowStock(predictedLow)
                .averageConfidence(round(avgConfidence))
                .build();

        // Sales trend (last 14 days revenue).
        List<ChartPoint> trend = saleRepository
                .dailySalesReport(today.minusDays(TREND_DAYS - 1L), today).stream()
                .map(p -> new ChartPoint(p.getSaleDate().toString(), p.getTotalAmount()))
                .toList();

        // Top products this month by revenue.
        List<ChartPoint> top = saleRepository.productWiseSales(monthStart, today).stream()
                .limit(TOP_PRODUCTS)
                .map(p -> new ChartPoint(p.getProductName(), p.getTotalAmount()))
                .toList();

        return DashboardResponse.builder()
                .totalProducts(totalProducts)
                .totalSuppliers(totalSuppliers)
                .lowStockProducts(lowStock)
                .monthlySales(monthlySales)
                .inventoryValue(inventoryValue)
                .predictionSummary(summary)
                .salesTrend(trend)
                .topProducts(top)
                .build();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
