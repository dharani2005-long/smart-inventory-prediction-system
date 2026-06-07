package com.smartinventory.controller;

import com.smartinventory.dto.forecast.ForecastResponse;
import com.smartinventory.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Prediction", description = "Smart inventory demand forecasting")
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @Operation(summary = "Forecast demand & reorder for a single product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<ForecastResponse> forecast(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer lookbackDays,
            @RequestParam(required = false) Integer horizonDays) {
        return ResponseEntity.ok(predictionService.forecast(productId, lookbackDays, horizonDays));
    }

    @Operation(summary = "Recompute forecasts for all products")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/run")
    public ResponseEntity<List<ForecastResponse>> runAll(
            @RequestParam(required = false) Integer lookbackDays,
            @RequestParam(required = false) Integer horizonDays) {
        return ResponseEntity.ok(predictionService.forecastAll(lookbackDays, horizonDays));
    }

    @Operation(summary = "Latest forecast per product")
    @GetMapping
    public ResponseEntity<List<ForecastResponse>> latest() {
        return ResponseEntity.ok(predictionService.latestForAll());
    }

    @Operation(summary = "Low-stock alerts & reorder recommendations")
    @GetMapping("/alerts")
    public ResponseEntity<List<ForecastResponse>> alerts() {
        return ResponseEntity.ok(predictionService.lowStockAlerts());
    }
}
