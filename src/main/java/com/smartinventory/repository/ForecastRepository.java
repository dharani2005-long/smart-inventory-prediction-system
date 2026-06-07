package com.smartinventory.repository;

import com.smartinventory.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, Long> {

    /** Most recent forecast snapshot for a product. */
    Optional<Forecast> findTopByProductIdOrderByGeneratedOnDescIdDesc(Long productId);

    /** Latest forecast per product (one row each), newest first. */
    @Query("""
            SELECT f FROM Forecast f
            WHERE f.id IN (
                SELECT MAX(f2.id) FROM Forecast f2 GROUP BY f2.product.id
            )
            ORDER BY f.lowStock DESC, f.depletionExpectedOn ASC
            """)
    List<Forecast> findLatestPerProduct();

    @Query("SELECT COUNT(DISTINCT f.product.id) FROM Forecast f WHERE f.lowStock = true " +
           "AND f.id IN (SELECT MAX(f2.id) FROM Forecast f2 GROUP BY f2.product.id)")
    long countLatestLowStock();
}
