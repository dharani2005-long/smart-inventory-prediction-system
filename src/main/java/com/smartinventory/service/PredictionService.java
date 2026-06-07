package com.smartinventory.service;

import com.smartinventory.dto.forecast.ForecastResponse;
import com.smartinventory.entity.Forecast;
import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.Sale;
import com.smartinventory.repository.ForecastRepository;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart Inventory Prediction module.
 *
 * <p>Uses simple moving-average forecasting over a lookback window of daily
 * sales to estimate future demand, stock depletion, and reorder needs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    public static final int DEFAULT_LOOKBACK_DAYS = 30;
    public static final int DEFAULT_HORIZON_DAYS = 30;
    public static final int SAFETY_DAYS = 7;          // safety-stock buffer

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ForecastRepository forecastRepository;
    private final ProductService productService;

    /** Compute, persist, and return a forecast for a single product. */
    @Transactional
    public ForecastResponse forecast(Long productId, Integer lookbackDays, Integer horizonDays) {
        Product product = productService.getEntity(productId);
        int lookback = lookbackDays != null && lookbackDays > 0 ? lookbackDays : DEFAULT_LOOKBACK_DAYS;
        int horizon = horizonDays != null && horizonDays > 0 ? horizonDays : DEFAULT_HORIZON_DAYS;

        Forecast forecast = computeAndSave(product, lookback, horizon);
        return ForecastResponse.from(forecast, buildRecommendation(forecast));
    }

    /** Recompute forecasts for all active products (batch run). */
    @Transactional
    public List<ForecastResponse> forecastAll(Integer lookbackDays, Integer horizonDays) {
        int lookback = lookbackDays != null && lookbackDays > 0 ? lookbackDays : DEFAULT_LOOKBACK_DAYS;
        int horizon = horizonDays != null && horizonDays > 0 ? horizonDays : DEFAULT_HORIZON_DAYS;

        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .map(p -> computeAndSave(p, lookback, horizon))
                .map(f -> ForecastResponse.from(f, buildRecommendation(f)))
                .toList();
    }

    /** Latest persisted forecast per product (recompute all if none exist yet). */
    @Transactional
    public List<ForecastResponse> latestForAll() {
        List<Forecast> latest = forecastRepository.findLatestPerProduct();
        if (latest.isEmpty()) {
            return forecastAll(DEFAULT_LOOKBACK_DAYS, DEFAULT_HORIZON_DAYS);
        }
        return latest.stream().map(f -> ForecastResponse.from(f, buildRecommendation(f))).toList();
    }

    /** Products that are (or are predicted to become) low on stock. */
    @Transactional
    public List<ForecastResponse> lowStockAlerts() {
        return forecastAll(DEFAULT_LOOKBACK_DAYS, DEFAULT_HORIZON_DAYS).stream()
                .filter(ForecastResponse::lowStock)
                .toList();
    }

    // ----------------------------------------------------------------------
    // Core algorithm
    // ----------------------------------------------------------------------

    private Forecast computeAndSave(Product product, int lookbackDays, int horizonDays) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(lookbackDays - 1L);

        List<Sale> sales = saleRepository.findByProductIdAndSaleDateBetween(product.getId(), start, today);

        // 1. Build the daily consumption series (0 on days without sales).
        double[] daily = buildDailySeries(sales, start, lookbackDays);

        // 2. Average daily consumption = moving average over the window.
        double total = 0;
        for (double d : daily) total += d;
        double avgDaily = total / lookbackDays;

        // 3. Forecast demand over the horizon.
        int forecastDemand = (int) Math.ceil(avgDaily * horizonDays);

        // 4. Compare with current stock.
        int currentStock = currentStock(product);
        LocalDate depletionDate = estimateDepletionDate(today, currentStock, avgDaily);

        int safetyStock = (int) Math.ceil(avgDaily * SAFETY_DAYS);
        int recommendedReorder = Math.max(0, forecastDemand + safetyStock - currentStock);

        boolean lowStock = currentStock <= product.getReorderLevel()
                || (forecastDemand > 0 && currentStock < forecastDemand);

        // 5. Confidence from data coverage and demand consistency.
        double confidence = computeConfidence(daily, avgDaily);

        Forecast forecast = Forecast.builder()
                .product(product)
                .generatedOn(today)
                .lookbackDays(lookbackDays)
                .avgDailyConsumption(round(avgDaily, 4))
                .forecastDemand(forecastDemand)
                .forecastHorizonDays(horizonDays)
                .currentStock(currentStock)
                .depletionExpectedOn(depletionDate)
                .recommendedReorderQty(recommendedReorder)
                .lowStock(lowStock)
                .confidencePercent(round(confidence, 2))
                .build();

        log.debug("Forecast for '{}': ADC={}, demand={}, stock={}, reorder={}, conf={}%",
                product.getName(), avgDaily, forecastDemand, currentStock, recommendedReorder, confidence);
        return forecastRepository.save(forecast);
    }

    /** Daily totals across the window; index 0 == start date. */
    private double[] buildDailySeries(List<Sale> sales, LocalDate start, int lookbackDays) {
        Map<Integer, Double> byDayIndex = new HashMap<>();
        for (Sale s : sales) {
            int idx = (int) ChronoUnit.DAYS.between(start, s.getSaleDate());
            if (idx >= 0 && idx < lookbackDays) {
                byDayIndex.merge(idx, (double) s.getQuantity(), Double::sum);
            }
        }
        double[] daily = new double[lookbackDays];
        for (int i = 0; i < lookbackDays; i++) {
            daily[i] = byDayIndex.getOrDefault(i, 0.0);
        }
        return daily;
    }

    private LocalDate estimateDepletionDate(LocalDate today, int currentStock, double avgDaily) {
        if (avgDaily <= 0 || currentStock <= 0) {
            return null; // no consumption or already empty → no meaningful depletion date
        }
        long daysToDeplete = (long) Math.floor(currentStock / avgDaily);
        return today.plusDays(daysToDeplete);
    }

    /**
     * Confidence (0–100) = 50% data-coverage + 50% demand-consistency.
     * Coverage = fraction of days in the window that had sales.
     * Consistency = 1 − coefficient of variation (clamped to [0,1]).
     */
    private double computeConfidence(double[] daily, double mean) {
        if (mean <= 0) return 0.0;

        int daysWithSales = 0;
        double sumSqDiff = 0;
        for (double d : daily) {
            if (d > 0) daysWithSales++;
            sumSqDiff += Math.pow(d - mean, 2);
        }
        double coverage = (double) daysWithSales / daily.length;

        double stdDev = Math.sqrt(sumSqDiff / daily.length);
        double cv = stdDev / mean;                       // coefficient of variation
        double consistency = Math.max(0.0, 1.0 - Math.min(cv, 1.0));

        double confidence = (0.5 * coverage + 0.5 * consistency) * 100.0;
        return Math.max(0.0, Math.min(100.0, confidence));
    }

    private int currentStock(Product product) {
        return inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getQuantityOnHand)
                .orElse(0);
    }

    private String buildRecommendation(Forecast f) {
        if (f.getRecommendedReorderQty() > 0) {
            String when = f.getDepletionExpectedOn() != null
                    ? " Estimated to run out by " + f.getDepletionExpectedOn() + "."
                    : "";
            return "REORDER %d units to cover the next %d days.%s"
                    .formatted(f.getRecommendedReorderQty(), f.getForecastHorizonDays(), when);
        }
        if (f.getAvgDailyConsumption() == 0) {
            return "No recent sales — no reorder needed. Review if this product is still active.";
        }
        return "Stock is sufficient for the forecast horizon. No action required.";
    }

    private double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
