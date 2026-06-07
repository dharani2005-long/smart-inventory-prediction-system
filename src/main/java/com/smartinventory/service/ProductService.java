package com.smartinventory.service;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.product.ProductRequest;
import com.smartinventory.dto.product.ProductResponse;
import com.smartinventory.entity.Category;
import com.smartinventory.entity.Inventory;
import com.smartinventory.entity.Product;
import com.smartinventory.entity.Supplier;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.InventoryRepository;
import com.smartinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryService categoryService;
    private final SupplierService supplierService;

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsBySku(req.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + req.sku());
        }
        if (StringUtils.hasText(req.barcode()) && productRepository.existsByBarcode(req.barcode())) {
            throw new DuplicateResourceException("Barcode already exists: " + req.barcode());
        }

        Product product = Product.builder()
                .name(req.name())
                .description(req.description())
                .sku(req.sku())
                .barcode(StringUtils.hasText(req.barcode()) ? req.barcode() : null)
                .unitPrice(req.unitPrice())
                .costPrice(req.costPrice())
                .reorderLevel(req.reorderLevel())
                .active(true)
                .category(resolveCategory(req.categoryId()))
                .supplier(resolveSupplier(req.supplierId()))
                .build();

        // 1:1 inventory row with optional opening stock.
        Inventory inventory = Inventory.builder()
                .product(product)
                .quantityOnHand(req.openingStock() != null ? req.openingStock() : 0)
                .reservedQuantity(0)
                .build();
        product.setInventory(inventory);

        product = productRepository.save(product);   // cascades inventory
        log.info("Created product '{}' (sku={})", product.getName(), product.getSku());
        return ProductResponse.from(product, product.getInventory());
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product product = getEntity(id);

        productRepository.findBySku(req.sku())
                .filter(p -> !p.getId().equals(id))
                .ifPresent(p -> { throw new DuplicateResourceException("SKU already exists: " + req.sku()); });
        if (StringUtils.hasText(req.barcode())) {
            productRepository.findByBarcode(req.barcode())
                    .filter(p -> !p.getId().equals(id))
                    .ifPresent(p -> { throw new DuplicateResourceException("Barcode already exists: " + req.barcode()); });
        }

        product.setName(req.name());
        product.setDescription(req.description());
        product.setSku(req.sku());
        product.setBarcode(StringUtils.hasText(req.barcode()) ? req.barcode() : null);
        product.setUnitPrice(req.unitPrice());
        product.setCostPrice(req.costPrice());
        product.setReorderLevel(req.reorderLevel());
        product.setCategory(resolveCategory(req.categoryId()));
        product.setSupplier(resolveSupplier(req.supplierId()));

        product = productRepository.save(product);
        return ProductResponse.from(product, product.getInventory());
    }

    @Transactional
    public void delete(Long id) {
        Product product = getEntity(id);
        // Soft-delete to preserve historical sales/transactions referencing this product.
        product.setActive(false);
        productRepository.save(product);
        log.info("Deactivated product id={}", id);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product p = getEntity(id);
        return ProductResponse.from(p, p.getInventory());
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Product p = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "barcode", barcode));
        return ProductResponse.from(p, p.getInventory());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword : null;
        return PageResponse.from(
                productRepository.search(kw, categoryId, pageable),
                p -> ProductResponse.from(p, p.getInventory()));
    }

    public Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private Category resolveCategory(Long categoryId) {
        return categoryId == null ? null : categoryService.getEntity(categoryId);
    }

    private Supplier resolveSupplier(Long supplierId) {
        return supplierId == null ? null : supplierService.getEntity(supplierId);
    }
}
