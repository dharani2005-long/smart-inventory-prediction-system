# Entity Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned
    CATEGORIES ||--o{ PRODUCTS : groups
    SUPPLIERS  ||--o{ PRODUCTS : supplies
    PRODUCTS   ||--|| INVENTORY : "current stock"
    PRODUCTS   ||--o{ STOCK_TRANSACTIONS : moves
    PRODUCTS   ||--o{ SALES : sold_as
    PRODUCTS   ||--o{ FORECASTS : forecast_for
    SUPPLIERS  ||--o{ STOCK_TRANSACTIONS : source
    USERS      ||--o{ STOCK_TRANSACTIONS : performed_by
    USERS      ||--o{ SALES : recorded_by

    USERS {
        bigint id PK
        string username UK
        string full_name
        string email UK
        string password
        bool   enabled
    }
    ROLES {
        bigint id PK
        string name UK
    }
    USER_ROLES {
        bigint user_id FK
        bigint role_id FK
    }
    CATEGORIES {
        bigint id PK
        string name UK
        string description
    }
    SUPPLIERS {
        bigint id PK
        string name
        string contact_person
        string email
        string phone
        string address
        bool   active
    }
    PRODUCTS {
        bigint id PK
        string name
        string sku UK
        string barcode UK
        decimal unit_price
        decimal cost_price
        int    reorder_level
        bool   active
        bigint category_id FK
        bigint supplier_id FK
    }
    INVENTORY {
        bigint id PK
        bigint product_id FK,UK
        int    quantity_on_hand
        int    reserved_quantity
        bigint version
    }
    STOCK_TRANSACTIONS {
        bigint id PK
        bigint product_id FK
        string type
        int    quantity
        int    balance_after
        string reference_no
        bigint supplier_id FK
        bigint performed_by FK
    }
    SALES {
        bigint id PK
        bigint product_id FK
        int    quantity
        decimal unit_price
        decimal total_amount
        date   sale_date
        string invoice_no
        bigint recorded_by FK
    }
    FORECASTS {
        bigint id PK
        bigint product_id FK
        date   generated_on
        int    lookback_days
        double avg_daily_consumption
        int    forecast_demand
        int    current_stock
        date   depletion_date
        int    recommended_reorder_qty
        bool   low_stock
        double confidence_percent
    }
```

## Relationship summary

| Relationship | Type | Notes |
|---|---|---|
| Users ↔ Roles | M:N | via `user_roles` join table |
| Category → Products | 1:N | a product belongs to one category |
| Supplier → Products | 1:N | a product has one primary supplier |
| Product ↔ Inventory | 1:1 | live stock level kept separate |
| Product → StockTransactions | 1:N | full movement history |
| Product → Sales | 1:N | demand history (forecast input) |
| Product → Forecasts | 1:N | prediction snapshots over time |
| User → Sales / Transactions | 1:N | audit of who recorded what |
```
