# Java Spring Microservices

Spring Boot Microservices POC with GraphQL, Service Discovery, API Gateway, and Background Jobs.

## Architecture

```
                         ┌─────────────────┐
                         │  Discovery Server│
                         │  (Eureka :8761)  │
                         └────────┬─────────┘
                                  │ register/discover
            ┌─────────────────────┼─────────────────────┐
            │                     │                     │
    ┌───────┴───────┐    ┌───────┴───────┐    ┌────────┴────────┐
    │Product Service │    │ Order Service │    │Inventory Service│
    │ GraphQL :8081  │    │ GraphQL :8082 │    │  REST    :8083  │
    └───────┬───────┘    └───┬───────┬───┘    └────────┬────────┘
            │                │       │                  │
            │                │  Feign│      WebClient   │
            │                │◄──────┼──────────────────┘
            │◄───────────────┘       │
            │                        │
    ┌───────┴────────────────────────┴───┐
    │          API Gateway (:8080)       │
    │       Spring Cloud Gateway         │
    └────────────────────────────────────┘
                      ▲
                      │ HTTP
                   Clients
```

## Tech Stack

| Technology             | Version    |
|------------------------|------------|
| Java                   | 21         |
| Spring Boot            | 3.4.1      |
| Spring Cloud           | 2024.0.0   |
| Spring for GraphQL     | (Boot managed) |
| Spring Cloud Gateway   | (Cloud managed) |
| Netflix Eureka         | (Cloud managed) |
| OpenFeign              | (Cloud managed) |
| H2 Database            | (Boot managed) |
| Lombok                 | (Boot managed) |
| Docker                 | Multi-stage |

## Services

| Service            | Port | Type        | Description                          |
|--------------------|------|-------------|--------------------------------------|
| discovery-server   | 8761 | Infrastructure | Eureka service registry           |
| api-gateway        | 8080 | Infrastructure | Spring Cloud Gateway              |
| product-service    | 8081 | Business    | Product catalog (GraphQL + REST)     |
| order-service      | 8082 | Business    | Order management (GraphQL)           |
| inventory-service  | 8083 | Business    | Stock tracking (REST, internal)      |

## Features

- **GraphQL API** — Schema-first approach with `@QueryMapping`, `@MutationMapping`, `@BatchMapping`
- **Service Discovery** — Eureka-based registration and discovery
- **API Gateway** — Automatic routing via Eureka discovery locator
- **Inter-Service Communication** — OpenFeign (declarative) + WebClient (reactive) with load balancing
- **Background Jobs** — `@Async` order processing + `@Scheduled` stale order cleanup
- **Virtual Threads** — Java 21 virtual threads enabled (`spring.threads.virtual.enabled=true`)
- **Production Docker** — Multi-stage builds, non-root user, health checks, JVM tuning

## Project Structure

```
java-spring-microservices/
├── pom.xml                          # Maven aggregator
├── docker-compose.yml               # Docker orchestration
├── discovery-server/                # Eureka Server
├── api-gateway/                     # Spring Cloud Gateway
├── product-service/                 # GraphQL + REST (internal)
│   └── src/main/resources/graphql/  # GraphQL schema
├── order-service/                   # GraphQL + background jobs
│   └── src/main/java/.../job/       # Async + Scheduled jobs
└── inventory-service/               # REST API (internal)
```

## How to Run

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for containerized run)

### Maven (Local)

Start services in order:

```bash
# 1. Discovery Server
cd discovery-server && mvn spring-boot:run &

# 2. API Gateway
cd api-gateway && mvn spring-boot:run &

# 3. Business services (after Eureka is up)
cd product-service && mvn spring-boot:run &
cd inventory-service && mvn spring-boot:run &
cd order-service && mvn spring-boot:run &
```

### Docker Compose

```bash
docker compose up --build
```

## API Endpoints

### Eureka Dashboard

```
http://localhost:8761
```

### GraphQL — Product Service

Direct: `http://localhost:8081/graphiql`
Via Gateway: `http://localhost:8080/product-service/graphiql`

**Query all products:**
```graphql
query {
  products {
    id
    name
    price
    category
  }
}
```

**Create a product:**
```graphql
mutation {
  createProduct(input: {
    name: "Wireless Mouse"
    description: "Ergonomic wireless mouse"
    price: 49.99
    category: "ACCESSORIES"
  }) {
    id
    name
    price
  }
}
```

### GraphQL — Order Service

Direct: `http://localhost:8082/graphiql`
Via Gateway: `http://localhost:8080/order-service/graphiql`

**Place an order:**
```graphql
mutation {
  placeOrder(input: {
    lineItems: [
      { productId: 1, quantity: 1 }
      { productId: 3, quantity: 2 }
    ]
  }) {
    id
    orderNumber
    status
    totalPrice
    lineItems {
      productId
      product {
        name
        price
      }
      quantity
      price
    }
  }
}
```

**Query orders by status:**
```graphql
query {
  ordersByStatus(status: PENDING) {
    id
    orderNumber
    status
    totalPrice
    createdAt
  }
}
```

### REST — Inventory Service (internal)

```bash
# Check stock
curl http://localhost:8083/api/inventory/1

# Via Gateway
curl http://localhost:8080/inventory-service/api/inventory/1
```

## Background Jobs

### Async Order Processing

When an order is placed via the `placeOrder` mutation:

1. Order is created with status `PENDING`
2. Inventory is decremented synchronously
3. `@Async` background job starts on a virtual thread
4. Simulates payment verification (configurable delay, default 10s)
5. Order status changes to `CONFIRMED`

### Scheduled Stale Order Cleanup

- Runs every 5 minutes (`order.cleanup.rate-ms`)
- Cancels orders stuck in `PENDING` for more than 30 minutes (`order.cleanup.stale-minutes`)
- Restores inventory for cancelled orders
- All timing is configurable via `application.yml`

## Inter-Service Communication

| From          | To                | Method    | Pattern         |
|---------------|-------------------|-----------|-----------------|
| order-service | product-service   | OpenFeign | Declarative HTTP |
| order-service | inventory-service | WebClient | Reactive HTTP    |
| api-gateway   | all services      | Gateway   | Eureka discovery |

## H2 Console

Each service exposes the H2 console for debugging:

| Service           | URL                              | JDBC URL              |
|-------------------|----------------------------------|-----------------------|
| product-service   | http://localhost:8081/h2-console | jdbc:h2:mem:productdb |
| order-service     | http://localhost:8082/h2-console | jdbc:h2:mem:orderdb   |
| inventory-service | http://localhost:8083/h2-console | jdbc:h2:mem:inventorydb |

## How to Run Tests

```bash
# All modules
mvn test

# Specific module
cd product-service && mvn test
```
