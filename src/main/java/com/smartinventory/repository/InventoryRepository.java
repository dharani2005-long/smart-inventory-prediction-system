package com.smartinventory.repository;

import com.smartinventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    /** Products whose on-hand quantity is at or below their reorder level. */
    @Query("""
            SELECT i FROM Inventory i
            JOIN i.product p
            WHERE p.active = true AND i.quantityOnHand <= p.reorderLevel
            ORDER BY i.quantityOnHand ASC
            """)
    List<Inventory> findLowStock();

    @Query("SELECT COUNT(i) FROM Inventory i JOIN i.product p " +
           "WHERE p.active = true AND i.quantityOnHand <= p.reorderLevel")
    long countLowStock();

    /** Total inventory value = sum(quantityOnHand * unitPrice). */
    @Query("SELECT COALESCE(SUM(i.quantityOnHand * p.unitPrice), 0) " +
           "FROM Inventory i JOIN i.product p WHERE p.active = true")
    java.math.BigDecimal totalInventoryValue();
}
