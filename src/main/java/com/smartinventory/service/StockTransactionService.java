package com.smartinventory.service;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.stock.StockTransactionRequest;
import com.smartinventory.dto.stock.StockTransactionResponse;
import com.smartinventory.entity.*;
import com.smartinventory.enums.TransactionType;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.StockTransactionRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransactionService {

    private final StockTransactionRepository transactionRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final UserRepository userRepository;

    /**
     * Records a stock movement and updates the product's on-hand quantity.
     */
    @Transactional
    public StockTransactionResponse record(StockTransactionRequest req) {
        Product product = productService.getEntity(req.productId());
        Supplier supplier = req.supplierId() != null ? supplierService.getEntity(req.supplierId()) : null;

        int delta = resolveDelta(req.type(), req.quantity());
        Inventory inventory = getOrCreateInventory(product);

        int newBalance = inventory.getQuantityOnHand() + delta;
        if (newBalance < 0) {
            throw new BusinessException(
                    "Insufficient stock for '%s'. On hand: %d, requested out: %d"
                            .formatted(product.getName(), inventory.getQuantityOnHand(), Math.abs(delta)));
        }

        inventory.setQuantityOnHand(newBalance);
        inventoryRepository.save(inventory);

        StockTransaction txn = StockTransaction.builder()
                .product(product)
                .type(req.type())
                .quantity(delta)
                .balanceAfter(newBalance)
                .referenceNo(req.referenceNo())
                .note(req.note())
                .supplier(supplier)
                .performedBy(currentUser())
                .build();
        txn = transactionRepository.save(txn);

        log.info("{} {} units of '{}' -> balance {}", req.type(), delta, product.getName(), newBalance);
        return StockTransactionResponse.from(txn);
    }

    /**
     * Internal helper used by the Sales module: decrements stock and logs a STOCK_OUT.
     * @return resulting on-hand balance.
     */
    @Transactional
    public int recordSaleStockOut(Product product, int quantity, String invoiceNo) {
        Inventory inventory = getOrCreateInventory(product);
        int newBalance = inventory.getQuantityOnHand() - quantity;
        if (newBalance < 0) {
            throw new BusinessException(
                    "Cannot sell %d of '%s'; only %d in stock"
                            .formatted(quantity, product.getName(), inventory.getQuantityOnHand()));
        }
        inventory.setQuantityOnHand(newBalance);
        inventoryRepository.save(inventory);

        StockTransaction txn = StockTransaction.builder()
                .product(product)
                .type(TransactionType.STOCK_OUT)
                .quantity(-quantity)
                .balanceAfter(newBalance)
                .referenceNo(invoiceNo)
                .note("Auto stock-out for sale")
                .performedBy(currentUser())
                .build();
        transactionRepository.save(txn);
        return newBalance;
    }

    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> historyByProduct(Long productId, Pageable pageable) {
        productService.getEntity(productId); // existence check
        return PageResponse.from(
                transactionRepository.findByProductId(productId, pageable),
                StockTransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> historyBySupplier(Long supplierId, Pageable pageable) {
        supplierService.getEntity(supplierId);
        return PageResponse.from(
                transactionRepository.findBySupplierId(supplierId, pageable),
                StockTransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> all(Pageable pageable) {
        return PageResponse.from(transactionRepository.findAll(pageable), StockTransactionResponse::from);
    }

    /** Translates a transaction type + quantity into a signed stock delta. */
    private int resolveDelta(TransactionType type, int quantity) {
        return switch (type) {
            case STOCK_IN, RETURN -> {
                if (quantity <= 0) throw new BusinessException(type + " quantity must be positive");
                yield quantity;
            }
            case STOCK_OUT -> {
                if (quantity <= 0) throw new BusinessException("STOCK_OUT quantity must be positive");
                yield -quantity;
            }
            case ADJUSTMENT -> {
                if (quantity == 0) throw new BusinessException("ADJUSTMENT quantity must be non-zero");
                yield quantity; // signed delta supplied directly
            }
        };
    }

    private Inventory getOrCreateInventory(Product product) {
        return inventoryRepository.findByProductId(product.getId())
                .orElseGet(() -> inventoryRepository.save(
                        Inventory.builder().product(product).quantityOnHand(0).reservedQuantity(0).build()));
    }

    private User currentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userRepository::findById)
                .orElse(null);
    }
}
