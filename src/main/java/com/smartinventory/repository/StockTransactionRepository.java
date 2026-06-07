package com.smartinventory.repository;

import com.smartinventory.entity.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    Page<StockTransaction> findByProductId(Long productId, Pageable pageable);

    Page<StockTransaction> findBySupplierId(Long supplierId, Pageable pageable);
}
