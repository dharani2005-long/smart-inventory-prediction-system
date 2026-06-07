package com.smartinventory.service;

import com.smartinventory.dto.stock.StockTransactionRequest;
import com.smartinventory.dto.stock.StockTransactionResponse;
import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.StockTransaction;
import com.smartinventory.enums.TransactionType;
import com.smartinventory.exception.BusinessException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.StockTransactionRepository;
import com.smartinventory.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockTransactionService — stock movements")
class StockTransactionServiceTest {

    @Mock private StockTransactionRepository transactionRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductService productService;
    @Mock private SupplierService supplierService;
    @Mock private UserRepository userRepository;

    @InjectMocks private StockTransactionService service;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("USB-C Cable").sku("ELEC-002").reorderLevel(50).build();
        inventory = Inventory.builder().product(product).quantityOnHand(10).reservedQuantity(0).build();
    }

    @Test
    @DisplayName("STOCK_IN increases on-hand quantity and records balanceAfter")
    void stockIn_increasesQuantity() {
        when(productService.getEntity(1L)).thenReturn(product);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArgument(0));

        var req = new StockTransactionRequest(1L, TransactionType.STOCK_IN, 20, null, "PO-1", "restock");
        StockTransactionResponse resp = service.record(req);

        assertThat(inventory.getQuantityOnHand()).isEqualTo(30);
        assertThat(resp.balanceAfter()).isEqualTo(30);
        assertThat(resp.quantity()).isEqualTo(20);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("STOCK_OUT beyond available stock throws BusinessException")
    void stockOut_insufficient_throws() {
        when(productService.getEntity(1L)).thenReturn(product);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

        var req = new StockTransactionRequest(1L, TransactionType.STOCK_OUT, 50, null, null, null);

        assertThatThrownBy(() -> service.record(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("negative ADJUSTMENT applies signed delta")
    void adjustment_appliesSignedDelta() {
        when(productService.getEntity(1L)).thenReturn(product);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArgument(0));

        var req = new StockTransactionRequest(1L, TransactionType.ADJUSTMENT, -4, null, null, "damaged");
        StockTransactionResponse resp = service.record(req);

        assertThat(inventory.getQuantityOnHand()).isEqualTo(6);
        assertThat(resp.balanceAfter()).isEqualTo(6);
    }
}
