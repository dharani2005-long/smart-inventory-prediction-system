# Smart Inventory Prediction System

A full-stack inventory management system for warehouses and retail stores that tracks
stock levels and **predicts future inventory requirements** using moving-average demand
forecasting.

**Stack:** Java 21 · Spring Boot 3.3 · Spring Security + JWT · Hibernate/JPA · MySQL ·
Maven · Apache POI · Swagger/OpenAPI · Bootstrap 5 + Chart.js (vanilla JS frontend).

---

## ✨ Features

| Module | Highlights |
|---|---|
| **Auth & RBAC** | Register, JWT login, roles `ADMIN` / `MANAGER` / `STAFF`, method-level security |
| **Inventory** | Product CRUD, categories, barcode/SKU, current stock, search + pagination |
| **Suppliers** | CRUD, link products, supplier history |
| **Stock** | Stock in / out / return / adjustment, full transaction history |
| **Sales** | Record sales (auto stock-out), daily / monthly / product-wise reports |
| **Prediction** | Moving-average forecast, depletion date, reorder qty, confidence %, alerts |
| **Dashboard** | KPIs, charts, prediction summary |
| **Reports** | Excel export (inventory, sales, suppliers, forecast) |

---

## 🚀 Quick Start

### Prerequisites
- JDK 21
- Maven 3.9+
- MySQL 8 running locally (a database `smart_inventory` is auto-created)

### 1. Configure the database
Defaults (override via environment variables) live in `src/main/resources/application.yml`:

```
DB_HOST=localhost  DB_PORT=3306  DB_NAME=smart_inventory
DB_USERNAME=root   DB_PASSWORD=root
```

> The schema is created automatically by Hibernate (`ddl-auto: update`).
> A reference DDL is in [`docs/schema.sql`](docs/schema.sql).

### 2. Run

```bash
# Windows PowerShell / macOS / Linux
mvn spring-boot:run
```

On first launch, **demo data is seeded automatically** (roles, users, products,
suppliers and ~60 days of sales history).

### 3. Open the app
- **Frontend:** http://localhost:8080/  (or `/login.html`)
- **Swagger UI:** http://localhost:8080/swagger-ui.html

### Demo accounts (password `password123`)
| Username | Role |
|---|---|
| `admin` | ADMIN |
| `manager` | MANAGER |
| `staff` | STAFF |

---

## 🔐 Authentication flow

1. `POST /api/auth/login` with `{ "usernameOrEmail": "admin", "password": "password123" }`
2. Copy the `accessToken` from the response.
3. Send it on every protected request: `Authorization: Bearer <token>`
   (In Swagger UI, click **Authorize** and paste the token.)

---

## 📡 API Endpoints

| Method | Path | Roles | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Register, returns JWT |
| POST | `/api/auth/login` | public | Login, returns JWT |
| GET/POST/PUT/DELETE | `/api/categories` | view: all · write: ADMIN/MANAGER · delete: ADMIN | Categories |
| GET/POST/PUT/DELETE | `/api/products` | same as above | Products / inventory |
| GET | `/api/products/barcode/{barcode}` | all | Lookup by barcode |
| GET/POST/PUT/DELETE | `/api/suppliers` | same as above | Suppliers |
| GET | `/api/suppliers/{id}/products` | all | Linked products |
| GET | `/api/suppliers/{id}/history` | all | Supplier transaction history |
| POST | `/api/stock-transactions` | ADMIN/MANAGER/STAFF | Record stock movement |
| GET | `/api/stock-transactions` | all | Transaction history (paged) |
| POST | `/api/sales` | ADMIN/MANAGER/STAFF | Record a sale |
| GET | `/api/sales/reports/daily` | all | Daily sales report |
| GET | `/api/sales/reports/monthly` | all | Monthly sales report |
| GET | `/api/sales/reports/product-wise` | all | Product-wise analysis |
| GET | `/api/predictions/product/{id}` | all | Forecast one product |
| POST | `/api/predictions/run` | ADMIN/MANAGER | Recompute all forecasts |
| GET | `/api/predictions` | all | Latest forecasts |
| GET | `/api/predictions/alerts` | all | Low-stock alerts & reorder recs |
| GET | `/api/dashboard` | all | KPIs + chart data |
| GET | `/api/reports/{inventory\|sales\|suppliers\|forecast}` | ADMIN/MANAGER | Excel export |

All list endpoints support pagination/sorting: `?page=0&size=10&sort=name,asc`.

---

## 🧮 Prediction Algorithm (Moving Average)

For each product, over a configurable **lookback window** (default 30 days):

1. Build a **daily consumption series** from sales (days with no sales = 0).
2. **Average Daily Consumption (ADC)** = total units sold ÷ window length.
3. **Forecast Demand** = `ADC × horizonDays` (default 30 days).
4. Compare with current stock:
   - **Depletion date** = `today + floor(currentStock / ADC)`
   - **Low-stock** when `stock ≤ reorderLevel` OR `stock < forecastDemand`
   - **Reorder qty** = `max(0, forecastDemand + safetyStock − currentStock)`,
     where `safetyStock = ADC × 7`
5. **Confidence %** = `50% × coverage + 50% × consistency`
   - *coverage* = fraction of window days that had sales
   - *consistency* = `1 − coefficient_of_variation` of daily demand

Implementation: [`PredictionService`](src/main/java/com/smartinventory/service/PredictionService.java).

---

## 🏗️ Project Structure (layered architecture)

```
src/main/java/com/smartinventory/
├── config/        SecurityConfig, JwtProperties, OpenApiConfig, DataSeeder
├── controller/    REST controllers (Auth, Product, Supplier, Stock, Sales, Prediction, Dashboard, Report)
├── service/       Business logic (incl. PredictionService, ExcelReportService)
├── repository/    Spring Data JPA repositories + projections
├── entity/        JPA entities (9 tables)
├── dto/           Request/response records, PageResponse
├── enums/         RoleName, TransactionType
├── security/      JwtService, JwtAuthenticationFilter, UserDetails, SecurityUtils
└── exception/     GlobalExceptionHandler + custom exceptions
src/main/resources/static/   Frontend (HTML/CSS/JS/Bootstrap)
src/test/java/...            JUnit 5 + Mockito tests
docs/                        schema.sql, ERD.md
```

---

## 🧪 Tests

```bash
mvn test
```

Tests run against in-memory **H2** (profile `test`) — no MySQL needed. Coverage includes
the prediction algorithm, stock-transaction rules, and auth/registration.

---

## 🛠️ Build a runnable JAR

```bash
mvn clean package
java -jar target/smart-inventory-prediction-system-1.0.0.jar
```

---

## ⚙️ Notes & Production Hardening
- **Change `APP_JWT_SECRET`** (Base64, ≥256-bit) in production.
- Set `ddl-auto: validate` and manage schema with migrations (Flyway/Liquibase).
- Restrict CORS origins in `SecurityConfig` to your frontend domain.
