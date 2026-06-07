package com.smartinventory.dto.dashboard;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated metrics + chart series for the dashboard landing page. */
@Builder
public record DashboardResponse(
        long totalProducts,
        long totalSuppliers,
        long lowStockProducts,
        BigDecimal monthlySales,
        BigDecimal inventoryValue,
        PredictionSummary predictionSummary,
        List<ChartPoint> salesTrend,      // last N days revenue
        List<ChartPoint> topProducts      // top products by revenue this month
) {

    @Builder
    public record PredictionSummary(
            long productsNeedingReorder,
            long productsPredictedLowStock,
            double averageConfidence
    ) {}

    /** Generic {label, value} point for charts. */
    public record ChartPoint(String label, BigDecimal value) {}
}
