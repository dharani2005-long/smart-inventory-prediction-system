package com.smartinventory.dto.forecast;

import com.smartinventory.entity.Forecast;
import lombok.Builder;

import java.time.LocalDate;

/** A demand-forecast result for one product. */
@Builder
public record ForecastResponse(
        Long productId,
        String productName,
        String sku,
        LocalDate generatedOn,
        int lookbackDays,
        double avgDailyConsumption,
        int forecastHorizonDays,
        int forecastDemand,
        int currentStock,
        LocalDate depletionExpectedOn,
        int recommendedReorderQty,
        boolean lowStock,
        double confidencePercent,
        String recommendation
) {
    public static ForecastResponse from(Forecast f, String recommendation) {
        return ForecastResponse.builder()
                .productId(f.getProduct().getId())
                .productName(f.getProduct().getName())
                .sku(f.getProduct().getSku())
                .generatedOn(f.getGeneratedOn())
                .lookbackDays(f.getLookbackDays())
                .avgDailyConsumption(f.getAvgDailyConsumption())
                .forecastHorizonDays(f.getForecastHorizonDays())
                .forecastDemand(f.getForecastDemand())
                .currentStock(f.getCurrentStock())
                .depletionExpectedOn(f.getDepletionExpectedOn())
                .recommendedReorderQty(f.getRecommendedReorderQty())
                .lowStock(f.isLowStock())
                .confidencePercent(f.getConfidencePercent())
                .recommendation(recommendation)
                .build();
    }
}
