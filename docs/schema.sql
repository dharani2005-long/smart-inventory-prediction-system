-- =====================================================================
-- Smart Inventory Prediction System — MySQL schema (reference)
-- Hibernate auto-generates these via ddl-auto; this file documents the
-- normalized design and can also be run manually.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS smart_inventory
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_inventory;

-- ---------------------------------------------------------------------
-- Roles & Users (RBAC, many-to-many)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE          -- ADMIN | MANAGER | STAFF
);

CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(120) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,         -- BCrypt hash
    enabled    BIT(1)       NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Categories & Suppliers
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  DATETIME,
    updated_at  DATETIME
);

CREATE TABLE suppliers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    contact_person VARCHAR(100),
    email          VARCHAR(120),
    phone          VARCHAR(20),
    address        VARCHAR(255),
    active         BIT(1) NOT NULL DEFAULT 1,
    created_at     DATETIME,
    updated_at     DATETIME
);

-- ---------------------------------------------------------------------
-- Products & Inventory (1:1)
-- ---------------------------------------------------------------------
CREATE TABLE products (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150)  NOT NULL,
    description   VARCHAR(500),
    sku           VARCHAR(60)   NOT NULL UNIQUE,
    barcode       VARCHAR(60)   UNIQUE,
    unit_price    DECIMAL(12,2) NOT NULL,
    cost_price    DECIMAL(12,2),
    reorder_level INT           NOT NULL DEFAULT 10,
    active        BIT(1)        NOT NULL DEFAULT 1,
    category_id   BIGINT,
    supplier_id   BIGINT,
    created_at    DATETIME,
    updated_at    DATETIME,
    CONSTRAINT fk_prod_cat FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_prod_sup FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE inventory (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT NOT NULL UNIQUE,
    quantity_on_hand  INT    NOT NULL DEFAULT 0,
    reserved_quantity INT    NOT NULL DEFAULT 0,
    version           BIGINT,                       -- optimistic lock
    created_at        DATETIME,
    updated_at        DATETIME,
    CONSTRAINT fk_inv_prod FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ---------------------------------------------------------------------
-- Stock transactions (audit log of movements)
-- ---------------------------------------------------------------------
CREATE TABLE stock_transactions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT NOT NULL,
    type          VARCHAR(20) NOT NULL,             -- STOCK_IN|STOCK_OUT|RETURN|ADJUSTMENT
    quantity      INT NOT NULL,                     -- signed delta
    balance_after INT NOT NULL,
    note          VARCHAR(255),
    reference_no  VARCHAR(60),
    supplier_id   BIGINT,
    performed_by  BIGINT,
    created_at    DATETIME,
    updated_at    DATETIME,
    CONSTRAINT fk_txn_prod FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_txn_sup  FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_txn_user FOREIGN KEY (performed_by) REFERENCES users(id),
    INDEX idx_txn_product (product_id),
    INDEX idx_txn_type (type)
);

-- ---------------------------------------------------------------------
-- Sales (historical demand data for forecasting)
-- ---------------------------------------------------------------------
CREATE TABLE sales (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT NOT NULL,
    quantity     INT NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL,
    sale_date    DATE NOT NULL,
    invoice_no   VARCHAR(60),
    recorded_by  BIGINT,
    created_at   DATETIME,
    updated_at   DATETIME,
    CONSTRAINT fk_sale_prod FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_sale_user FOREIGN KEY (recorded_by) REFERENCES users(id),
    INDEX idx_sale_product (product_id),
    INDEX idx_sale_date (sale_date)
);

-- ---------------------------------------------------------------------
-- Forecasts (persisted prediction snapshots)
-- ---------------------------------------------------------------------
CREATE TABLE forecasts (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id              BIGINT NOT NULL,
    generated_on            DATE NOT NULL,
    lookback_days           INT NOT NULL,
    avg_daily_consumption   DOUBLE NOT NULL,
    forecast_demand         INT NOT NULL,
    forecast_horizon_days   INT NOT NULL,
    current_stock           INT NOT NULL,
    depletion_date          DATE,
    recommended_reorder_qty INT NOT NULL,
    low_stock               BIT(1) NOT NULL,
    confidence_percent      DOUBLE NOT NULL,
    created_at              DATETIME,
    updated_at              DATETIME,
    CONSTRAINT fk_fc_prod FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_forecast_product (product_id),
    INDEX idx_forecast_date (generated_on)
);
