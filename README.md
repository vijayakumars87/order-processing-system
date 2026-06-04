# Order Processing System

Backend for an e-commerce order processing system, built with **Java 21 + Spring Boot 3**.
Customers can place orders with multiple items, track their status, list/filter orders,
and cancel orders while they are still `PENDING`. A background job promotes `PENDING`
orders to `PROCESSING` every 5 minutes.

## Tech stack

| Concern         | Choice                                   |
|-----------------|------------------------------------------|
| Language        | Java 21                                  |
| Framework       | Spring Boot 3.3 (Web, Data JPA, Validation, Security, Scheduling) |
| Database        | H2 in-memory (zero setup to run)         |
| Auth            | JWT (jjwt) + Spring Security, role-based |
| API docs        | springdoc-openapi (Swagger UI)           |
| Build           | Gradle (wrapper included)                |
| Tests           | JUnit 5, Mockito, Spring MockMvc         |

## Project structure

```
src/main/java/com/assignment/orderprocessing
├── OrderProcessingApplication.java   # entrypoint (@EnableScheduling)
├── config/        # OpenAPI config, data seeder
├── controller/    # REST controllers (Auth, Orders, Products)
├── domain/        # JPA entities + OrderStatus state machine + Role
├── dto/           # request/response records + mapper
├── exception/     # custom exceptions + global handler
├── repository/    # Spring Data JPA repositories
├── scheduler/     # PENDING -> PROCESSING background job
├── security/      # JWT service, filter, security config
└── service/       # business logic (OrderService, ProductService, AuthService)
```

## Running

```bash
./gradlew bootRun
```

App starts on `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:orderdb`, user `sa`, no password)

### Seeded demo data (dev only)

On first startup (when the DB is empty) `DataSeeder` loads a realistic dataset.
H2 is in-memory, so a fresh dataset is seeded on every boot.

**Users** (password is `<username>123`):

| Username   | Password      | Role     | Notes                                  |
|------------|---------------|----------|----------------------------------------|
| `admin`    | `admin123`    | ADMIN    |                                        |
| `customer` | `customer123` | CUSTOMER |                                        |
| `rahul`    | `rahul123`    | CUSTOMER |                                        |
| `priya`    | `priya123`    | CUSTOMER |                                        |
| `blocked`  | `blocked123`  | CUSTOMER | `enabled=false` — login is rejected    |

**Products (12):** iPhone 15, Samsung Galaxy S24, Air Conditioner 1.5 Ton,
Dell XPS 15, MacBook Air M3, 55" 4K OLED TV, Refrigerator, Washing Machine,
Sony WH-1000XM5, iPad Air, Microwave Oven, Logitech MX Mouse — each with a
realistic price and stock level.

**Orders (6):** one per status so every flow is testable immediately —
2× `PENDING`, 1× `PROCESSING`, 1× `SHIPPED`, 1× `DELIVERED`, 1× `CANCELLED`,
across different customers, including multi-item orders.

## Authentication

1. `POST /api/auth/register` to create a user. `role` is optional and defaults to
   `CUSTOMER`. All other fields are required:
   ```json
   {
     "username": "newuser",
     "password": "secret1",
     "email": "new@example.com",
     "fullName": "New User",
     "mobileNumber": "+919811112222",
     "role": "CUSTOMER"
   }
   ```
2. `POST /api/auth/login` with `{ "username": "...", "password": "..." }` → returns a JWT.
   Bad credentials or a disabled account return `401`.
3. Send `Authorization: Bearer <token>` on all `/api/orders` and `/api/products` calls.

## Order status state machine

```
PENDING ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
   │
   └──▶ CANCELLED        (only PENDING orders may be cancelled)
```

Illegal transitions return `409 Conflict`. The 5-minute scheduler only moves
`PENDING → PROCESSING`.

## API summary

| Method | Path                        | Auth        | Description                                   |
|--------|-----------------------------|-------------|-----------------------------------------------|
| POST   | `/api/auth/register`        | public      | Register a user                               |
| POST   | `/api/auth/login`           | public      | Login, returns JWT                            |
| POST   | `/api/products`             | ADMIN       | Create a product                              |
| GET    | `/api/products`             | any auth    | List products                                 |
| GET    | `/api/products/{id}`        | any auth    | Get product                                   |
| POST   | `/api/orders`               | any auth    | Place an order with multiple items            |
| GET    | `/api/orders/{id}`          | owner/ADMIN | Get order details                             |
| GET    | `/api/orders?status=`       | any auth    | List orders (ADMIN: all; CUSTOMER: own), filter by status |
| PATCH  | `/api/orders/{id}/status`   | ADMIN       | Update status (validated against state machine) |
| POST   | `/api/orders/{id}/cancel`   | owner/ADMIN | Cancel order (only if PENDING)                |

### Example: place an order

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"customer","password":"customer123"}' | jq -r .token)

curl -X POST localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'
```

## Testing

```bash
./gradlew test
```

Coverage:
- `OrderStatusTest` — state-machine transition rules (unit).
- `OrderServiceTest` — business logic with Mockito (create, stock, cancel rules, illegal transitions).
- `OrderStatusSchedulerTest` — verifies the background job promotes only `PENDING` orders.
- `OrderApiIntegrationTest` — full HTTP flow with security: auth, ownership, role checks, validation, cancel rules.

## Design notes

- **Validation:** request DTOs use Jakarta Bean Validation (e.g. `@Email`, `@Positive`,
  phone `@Pattern`); failures return `400` with per-field messages via the global handler.
- **Stock handling:** stock is reserved on order creation and restored on cancellation.
- **Ownership:** customers can only see/cancel their own orders; admins can see all and drive status changes.
- **Scheduler cron** is externalised (`order.scheduler.pending-to-processing-cron`) so tests disable it and invoke the job directly for determinism.
- **JWT secret** is read from `APP_JWT_SECRET`; the committed default is for local dev only.

See [AI_USAGE.md](AI_USAGE.md) for the required write-up on AI tool usage.
