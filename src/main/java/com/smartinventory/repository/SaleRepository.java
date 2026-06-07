package com.smartinventory.repository;

import com.smartinventory.entity.Sale;
import com.smartinventory.repository.projection.DailySalesProjection;
import com.smartinventory.repository.projection.MonthlySalesProjection;
import com.smartinventory.repository.projection.ProductSalesProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByProductId(Long productId, Pageable pageable);

    Page<Sale> findBySaleDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    /** All sales of a product within a window — raw input for the forecast. */
    List<Sale> findByProductIdAndSaleDateBetween(Long productId, LocalDate start, LocalDate end);

    /** Total units sold of a product in a window. */
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s " +
           "WHERE s.product.id = :productId AND s.saleDate BETWEEN :start AND :end")
    long totalQuantitySold(@Param("productId") Long productId,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);

    /** Total sales revenue within a window (dashboard / monthly report). */
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
           "WHERE s.saleDate BETWEEN :start AND :end")
    BigDecimal totalRevenue(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Daily sales report between two dates. */
    @Query("""
            SELECT s.saleDate AS saleDate,
                   SUM(s.quantity) AS totalQuantity,
                   SUM(s.totalAmount) AS totalAmount,
                   COUNT(s) AS transactionCount
            FROM Sale s
            WHERE s.saleDate BETWEEN :start AND :end
            GROUP BY s.saleDate
            ORDER BY s.saleDate
            """)
    List<DailySalesProjection> dailySalesReport(@Param("start") LocalDate start,
                                                @Param("end") LocalDate end);

    /** Monthly sales report for a year. */
    @Query("""
            SELECT YEAR(s.saleDate) AS year,
                   MONTH(s.saleDate) AS month,
                   SUM(s.quantity) AS totalQuantity,
                   SUM(s.totalAmount) AS totalAmount
            FROM Sale s
            WHERE YEAR(s.saleDate) = :year
            GROUP BY YEAR(s.saleDate), MONTH(s.saleDate)
            ORDER BY MONTH(s.saleDate)
            """)
    List<MonthlySalesProjection> monthlySalesReport(@Param("year") int year);

    /** Product-wise sales analysis within a window. */
    @Query("""
            SELECT p.id AS productId,
                   p.name AS productName,
                   p.sku AS sku,
                   SUM(s.quantity) AS totalQuantity,
                   SUM(s.totalAmount) AS totalAmount
            FROM Sale s JOIN s.product p
            WHERE s.saleDate BETWEEN :start AND :end
            GROUP BY p.id, p.name, p.sku
            ORDER BY SUM(s.totalAmount) DESC
            """)
    List<ProductSalesProjection> productWiseSales(@Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);
}
