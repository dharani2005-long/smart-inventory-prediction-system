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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PredictionService — moving-average forecasting")
class PredictionServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ForecastRepository forecastRepository;
    @Mock private ProductService productService;

    @InjectMocks private PredictionService predictionService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L).name("Wireless Mouse").sku("ELEC-001")
                .unitPrice(new BigDecimal("25.00")).reorderLevel(10).active(true)
                .build();
    }

    @Test
    @DisplayName("computes ADC, 30-day demand, reorder qty and 100% confidence for steady sales")
    void steadyDemand_producesExpectedForecast() {
        int lookback = 30, horizon = 30, currentStock = 50, dailyQty = 5;

        // Steady 5 units/day for the whole 30-day window.
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(lookback - 1L);
        List<Sale> sales = new ArrayList<>();
        for (int i = 0; i < lookback; i++) {
            sales.add(Sale.builder()
                    .product(product).quantity(dailyQty)
                    .unitPrice(product.getUnitPrice())
                    .totalAmount(product.getUnitPrice().multiply(BigDecimal.valueOf(dailyQty)))
                    .saleDate(start.plusDays(i)).build());
        }

        when(productService.getEntity(1L)).thenReturn(product);
        when(saleRepository.findByProductIdAndSaleDateBetween(anyLong(), any(), any())).thenReturn(sales);
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(Inventory.builder().product(product).quantityOnHand(currentStock).build()));
        when(forecastRepository.save(any(Forecast.class))).thenAnswer(inv -> inv.getArgument(0));

        ForecastResponse result = predictionService.forecast(1L, lookback, horizon);

        // ADC = 150 / 30 = 5.0
        assertThat(result.avgDailyConsumption()).isEqualTo(5.0);
        // Forecast demand = 5 * 30 = 150
        assertThat(result.forecastDemand()).isEqualTo(150);
        // Reorder = demand(150) + safety(5*7=35) - stock(50) = 135
        assertThat(result.recommendedReorderQty()).isEqualTo(135);
        // 50 in stock < 150 forecast demand -> low stock
        assertThat(result.lowStock()).isTrue();
        // Depletion = today + floor(50/5) = today + 10 days
        assertThat(result.depletionExpectedOn()).isEqualTo(today.plusDays(10));
        // Steady, full-coverage data -> max confidence
        assertThat(result.confidencePercent()).isEqualTo(100.0);
        assertThat(result.recommendation()).contains("REORDER 135");
    }

    @Test
    @DisplayName("no sales history -> zero demand, zero confidence, no reorder")
    void noSales_producesZeroForecast() {
        when(productService.getEntity(1L)).thenReturn(product);
        when(saleRepository.findByProductIdAndSaleDateBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(Inventory.builder().product(product).quantityOnHand(100).build()));
        when(forecastRepository.save(any(Forecast.class))).thenAnswer(inv -> inv.getArgument(0));

        ForecastResponse result = predictionService.forecast(1L, 30, 30);

        assertThat(result.avgDailyConsumption()).isZero();
        assertThat(result.forecastDemand()).isZero();
        assertThat(result.recommendedReorderQty()).isZero();
        assertThat(result.confidencePercent()).isZero();
        assertThat(result.depletionExpectedOn()).isNull();
        assertThat(result.lowStock()).isFalse();
    }

    @Test
    @DisplayName("stock below reorder level is flagged low even with light demand")
    void belowReorderLevel_flaggedLowStock() {
        product.setReorderLevel(40);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);
        List<Sale> sales = List.of(Sale.builder()
                .product(product).quantity(2).unitPrice(product.getUnitPrice())
                .totalAmount(new BigDecimal("50.00")).saleDate(start.plusDays(5)).build());

        when(productService.getEntity(1L)).thenReturn(product);
        when(saleRepository.findByProductIdAndSaleDateBetween(anyLong(), any(), any())).thenReturn(sales);
        when(inventoryRepository.findByProductId(1L))
                .thenReturn(Optional.of(Inventory.builder().product(product).quantityOnHand(30).build()));
        when(forecastRepository.save(any(Forecast.class))).thenAnswer(inv -> inv.getArgument(0));

        ForecastResponse result = predictionService.forecast(1L, 30, 30);

        assertThat(result.lowStock()).isTrue();          // 30 <= reorderLevel 40
        assertThat(result.confidencePercent()).isBetween(0.0, 100.0);
    }
}
