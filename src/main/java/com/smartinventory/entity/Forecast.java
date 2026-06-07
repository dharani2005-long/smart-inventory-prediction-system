package com.smartinventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A persisted demand-forecast snapshot for a product, produced by the
 * Smart Inventory Prediction module using moving-average forecasting.
 */
@Entity
@Table(name = "forecasts", indexes = {
        @Index(name = "idx_forecast_product", columnList = "product_id"),
        @Index(name = "idx_forecast_date", columnList = "generated_on")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Forecast extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "generated_on", nullable = false)
    private LocalDate generatedOn;

    /** Window (in days) of sales history used for the moving average. */
    @Column(nullable = false)
    private Integer lookbackDays;

    /** Average units consumed per day over the lookback window. */
    @Column(nullable = false)
    private Double avgDailyConsumption;

    /** Forecast demand over the next {@code forecastHorizonDays}. */
    @Column(nullable = false)
    private Integer forecastDemand;

    @Column(nullable = false)
    private Integer forecastHorizonDays;

    @Column(nullable = false)
    private Integer currentStock;

    /** Estimated date stock reaches zero (null if no consumption). */
    @Column(name = "depletion_date")
    private LocalDate depletionExpectedOn;

    /** Suggested quantity to reorder to cover the horizon plus a safety buffer. */
    @Column(nullable = false)
    private Integer recommendedReorderQty;

    @Column(nullable = false)
    private boolean lowStock;

    /** Confidence in the forecast, 0–100, based on history depth & variance. */
    @Column(nullable = false)
    private Double confidencePercent;
}
