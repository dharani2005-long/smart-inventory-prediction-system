package com.smartinventory.config;

import com.smartinventory.entity.*;
import com.smartinventory.enums.RoleName;
import com.smartinventory.enums.TransactionType;
import com.smartinventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Seeds reference + demo data on first startup so the system (and the
 * prediction engine) is usable immediately. Idempotent: skips if users exist.
 *
 * <p>Default accounts (password = {@code password123}):
 * <ul><li>admin / ADMIN</li><li>manager / MANAGER</li><li>staff / STAFF</li></ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    // Deterministic randomness so seeded sales are reproducible across runs.
    private final Random random = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        if (userRepository.count() > 0) {
            log.info("Data already seeded — skipping demo data.");
            return;
        }
        Map<RoleName, Role> roles = seedRoles();
        User admin = seedUsers(roles);
        List<Category> categories = seedCategories();
        List<Supplier> suppliers = seedSuppliers();
        List<Product> products = seedProducts(categories, suppliers, admin);
        seedSalesHistory(products, admin);
        log.info("✅ Demo data seeded: {} products, {} suppliers, sample sales generated.",
                products.size(), suppliers.size());
    }

    private Map<RoleName, Role> seedRoles() {
        Map<RoleName, Role> map = new EnumMap<>(RoleName.class);
        for (RoleName rn : RoleName.values()) {
            Role role = roleRepository.findByName(rn)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(rn).build()));
            map.put(rn, role);
        }
        return map;
    }

    private User seedUsers(Map<RoleName, Role> roles) {
        User admin = createUser("admin", "System Admin", "admin@smartinventory.com",
                Set.of(roles.get(RoleName.ADMIN)));
        createUser("manager", "Store Manager", "manager@smartinventory.com",
                Set.of(roles.get(RoleName.MANAGER)));
        createUser("staff", "Floor Staff", "staff@smartinventory.com",
                Set.of(roles.get(RoleName.STAFF)));
        return admin;
    }

    private User createUser(String username, String fullName, String email, Set<Role> roles) {
        User user = User.builder()
                .username(username)
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .roles(new HashSet<>(roles))
                .build();
        return userRepository.save(user);
    }

    private List<Category> seedCategories() {
        List<Category> categories = List.of(
                Category.builder().name("Electronics").description("Electronic devices & accessories").build(),
                Category.builder().name("Groceries").description("Food & household items").build(),
                Category.builder().name("Stationery").description("Office & school supplies").build(),
                Category.builder().name("Apparel").description("Clothing & accessories").build());
        return categoryRepository.saveAll(categories);
    }

    private List<Supplier> seedSuppliers() {
        List<Supplier> suppliers = List.of(
                Supplier.builder().name("Acme Distributors").contactPerson("John Doe")
                        .email("sales@acme.com").phone("555-1001").address("12 Industrial Rd").active(true).build(),
                Supplier.builder().name("Global Supplies Co").contactPerson("Jane Smith")
                        .email("contact@globalsupplies.com").phone("555-1002").address("88 Market St").active(true).build(),
                Supplier.builder().name("Prime Wholesale").contactPerson("Bob Lee")
                        .email("orders@primewholesale.com").phone("555-1003").address("5 Dock Ave").active(true).build());
        return supplierRepository.saveAll(suppliers);
    }

    private List<Product> seedProducts(List<Category> categories, List<Supplier> suppliers, User admin) {
        Object[][] defs = {
                // name, sku, barcode, unitPrice, costPrice, reorder, openingStock, catIdx, supIdx
                {"Wireless Mouse", "ELEC-001", "8901234500011", "25.00", "15.00", 20, 120, 0, 0},
                {"USB-C Cable", "ELEC-002", "8901234500028", "9.50", "4.00", 50, 300, 0, 0},
                {"Bluetooth Speaker", "ELEC-003", "8901234500035", "45.00", "28.00", 15, 60, 0, 1},
                {"Rice 5kg", "GROC-001", "8901234500042", "12.00", "8.00", 40, 200, 1, 1},
                {"Cooking Oil 1L", "GROC-002", "8901234500059", "6.50", "4.50", 60, 80, 1, 2},
                {"A4 Paper Ream", "STAT-001", "8901234500066", "5.00", "3.00", 30, 150, 2, 2},
                {"Ballpoint Pen Pack", "STAT-002", "8901234500073", "3.20", "1.50", 80, 25, 2, 0},
                {"Cotton T-Shirt", "APP-001", "8901234500080", "15.00", "8.00", 25, 90, 3, 1},
        };
        List<Product> products = new ArrayList<>();
        for (Object[] d : defs) {
            Product p = Product.builder()
                    .name((String) d[0]).sku((String) d[1]).barcode((String) d[2])
                    .unitPrice(new BigDecimal((String) d[3])).costPrice(new BigDecimal((String) d[4]))
                    .reorderLevel((Integer) d[5]).active(true)
                    .category(categories.get((Integer) d[7]))
                    .supplier(suppliers.get((Integer) d[8]))
                    .build();
            int opening = (Integer) d[6];
            Inventory inv = Inventory.builder().product(p).quantityOnHand(opening).reservedQuantity(0).build();
            p.setInventory(inv);
            p = productRepository.save(p);

            // opening stock-in transaction for the audit trail
            stockTransactionRepository.save(StockTransaction.builder()
                    .product(p).type(TransactionType.STOCK_IN).quantity(opening)
                    .balanceAfter(opening).note("Opening stock").performedBy(admin).build());
            products.add(p);
        }
        return products;
    }

    /** Generate ~60 days of sales with per-product baseline demand + noise. */
    private void seedSalesHistory(List<Product> products, User admin) {
        LocalDate today = LocalDate.now();
        int[] baselineDemand = {4, 8, 2, 6, 3, 5, 7, 3}; // avg units/day per product

        for (int p = 0; p < products.size(); p++) {
            Product product = products.get(p);
            int base = baselineDemand[p % baselineDemand.length];
            Inventory inv = product.getInventory();

            for (int dayAgo = 60; dayAgo >= 1; dayAgo--) {
                // weekend uplift + random noise; occasional zero-sales days
                LocalDate date = today.minusDays(dayAgo);
                int qty = base + random.nextInt(Math.max(1, base / 2 + 1));
                if (date.getDayOfWeek().getValue() >= 6) qty += base / 2; // Sat/Sun bump
                if (random.nextInt(10) == 0) qty = 0; // ~10% no-sale days
                if (qty <= 0) continue;
                if (inv.getQuantityOnHand() < qty) continue; // don't oversell historically

                BigDecimal total = product.getUnitPrice().multiply(BigDecimal.valueOf(qty));
                saleRepository.save(Sale.builder()
                        .product(product).quantity(qty)
                        .unitPrice(product.getUnitPrice()).totalAmount(total)
                        .saleDate(date).invoiceNo("INV-" + product.getSku() + "-" + dayAgo)
                        .recordedBy(admin).build());
                inv.setQuantityOnHand(inv.getQuantityOnHand() - qty);
            }
            inventoryRepository.save(inv);
        }
    }
}
