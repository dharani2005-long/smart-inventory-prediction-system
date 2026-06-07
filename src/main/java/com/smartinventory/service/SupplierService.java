package com.smartinventory.service;

import com.smartinventory.dto.PageResponse;
import com.smartinventory.dto.product.ProductResponse;
import com.smartinventory.dto.supplier.SupplierRequest;
import com.smartinventory.dto.supplier.SupplierResponse;
import com.smartinventory.entity.Supplier;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    @Transactional
    public SupplierResponse create(SupplierRequest req) {
        if (supplierRepository.existsByNameIgnoreCase(req.name())) {
            throw new DuplicateResourceException("Supplier already exists: " + req.name());
        }
        Supplier supplier = Supplier.builder()
                .name(req.name())
                .contactPerson(req.contactPerson())
                .email(req.email())
                .phone(req.phone())
                .address(req.address())
                .active(true)
                .build();
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest req) {
        Supplier supplier = getEntity(id);
        supplier.setName(req.name());
        supplier.setContactPerson(req.contactPerson());
        supplier.setEmail(req.email());
        supplier.setPhone(req.phone());
        supplier.setAddress(req.address());
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = getEntity(id);
        // Deactivate to keep referential integrity with products/transactions.
        supplier.setActive(false);
        supplierRepository.save(supplier);
        log.info("Deactivated supplier id={}", id);
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        return SupplierResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(String keyword, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword : "";
        return PageResponse.from(
                supplierRepository.findByNameContainingIgnoreCase(kw, pageable),
                SupplierResponse::from);
    }

    /** Products linked to a supplier (supplier history / catalog). */
    @Transactional(readOnly = true)
    public List<ProductResponse> productsOfSupplier(Long supplierId) {
        getEntity(supplierId); // existence check
        return productRepository.findBySupplierId(supplierId).stream()
                .map(p -> ProductResponse.from(p, p.getInventory()))
                .toList();
    }

    public Supplier getEntity(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
    }
}
