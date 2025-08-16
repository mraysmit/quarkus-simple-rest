# Quarkus Simple REST Application

A comprehensive trading system built with Quarkus, featuring REST APIs, database persistence, comprehensive monitoring, and testing infrastructure.

## 🚀 Features

- **REST API**: Complete trading and counterparty management
- **Database Integration**: PostgreSQL with H2 for testing
- **Monitoring**: Prometheus metrics and Grafana dashboards
- **Testing**: TestContainers for integration testing
- **Health Checks**: Application health monitoring
- **Validation**: Comprehensive input validation
- **Error Handling**: Global exception handling
- **Code Quality**: Clean architecture with builder patterns

## 📋 Table of Contents

- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Database](#database)
- [Monitoring & Metrics](#monitoring--metrics)
- [Testing](#testing)
- [Development](#development)
- [Deployment](#deployment)
- [Architecture](#architecture)

## 🏃 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for TestContainers and monitoring)

### Running the Application

1. **Development Mode** (with live reload):
   ```bash
   ./mvnw quarkus:dev
   ```
   Access the application at: http://localhost:8080

2. **Production Mode**:
   ```bash
   ./mvnw package
   java -jar target/quarkus-app/quarkus-run.jar
   ```

3. **Dev UI** (development mode only):
   http://localhost:8080/q/dev/

### Quick API Test

```bash
# Create a counterparty
curl -X POST http://localhost:8080/api/counterparties \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Goldman Sachs",
    "code": "GS001",
    "email": "trading@gs.com",
    "type": "INSTITUTIONAL"
  }'

# Create a trade
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "tradeReference": "TRD-001",
    "counterpartyId": 1,
    "instrument": "AAPL",
    "tradeType": "BUY",
    "quantity": 100,
    "price": 150.00,
    "tradeDate": "2023-12-01",
    "settlementDate": "2023-12-03",
    "currency": "USD"
  }'
```

## 📚 API Documentation

### Counterparty Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/counterparties` | List all counterparties |
| GET | `/api/counterparties/{id}` | Get counterparty by ID |
| POST | `/api/counterparties` | Create new counterparty |
| PUT | `/api/counterparties/{id}` | Update counterparty |
| DELETE | `/api/counterparties/{id}` | Delete counterparty |
| GET | `/api/counterparties/stats/count` | Get counterparty statistics |

### Trade Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/trades` | List all trades (with pagination) |
| GET | `/api/trades/{id}` | Get trade by ID |
| POST | `/api/trades` | Create new trade |
| PATCH | `/api/trades/{id}/status` | Update trade status |
| GET | `/api/trades/pending` | Get pending trades |
| GET | `/api/trades/reference/{ref}` | Get trade by reference |

### Health & Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/q/health` | Application health check |
| GET | `/q/health/live` | Liveness probe |
| GET | `/q/health/ready` | Readiness probe |
| GET | `/q/metrics` | Prometheus metrics |

### Sample API Requests

<details>
<summary>Create Counterparty</summary>

```json
POST /api/counterparties
{
  "name": "JPMorgan Chase",
  "code": "JPM001",
  "email": "trading@jpmorgan.com",
  "type": "INSTITUTIONAL",
  "status": "ACTIVE"
}
```
</details>

<details>
<summary>Create Trade</summary>

```json
POST /api/trades
{
  "tradeReference": "TRD-BUY-001",
  "counterpartyId": 1,
  "instrument": "GOOGL",
  "tradeType": "BUY",
  "quantity": 50,
  "price": 2800.75,
  "tradeDate": "2023-12-01",
  "settlementDate": "2023-12-03",
  "currency": "USD"
}
```
</details>

## 🗄️ Database

### Configuration

The application supports multiple database configurations:

- **Development**: PostgreSQL (default)
- **Testing**: H2 in-memory database
- **TestContainers**: Real PostgreSQL for integration tests

### Database Schema

**Counterparties Table:**
- `id` (Primary Key)
- `name` (Unique)
- `code` (Unique)
- `email`
- `type` (INSTITUTIONAL, CORPORATE, INDIVIDUAL)
- `status` (ACTIVE, INACTIVE, SUSPENDED)
- `created_at`, `updated_at`

**Trades Table:**
- `id` (Primary Key)
- `trade_reference` (Unique)
- `counterparty_id` (Foreign Key)
- `instrument`
- `trade_type` (BUY, SELL)
- `quantity`
- `price`
- `trade_date`
- `settlement_date`
- `currency`
- `status` (PENDING, CONFIRMED, SETTLED, CANCELLED)
- `created_at`, `updated_at`

### Sample Data

The application automatically creates sample data on startup:
- 3 sample counterparties
- 3 sample trades with different statuses

## 📊 Monitoring & Metrics

### Prometheus Integration

The application exposes comprehensive metrics at `/q/metrics`:

**Business Metrics:**
```
# Trade operations
trading_trades_created_total{instrument="AAPL",type="BUY"}
trading_trades_confirmed_total{instrument="AAPL",type="BUY"}
trading_trades_settled_total{instrument="AAPL",type="BUY"}
trading_trades_failed_total{instrument="AAPL",type="BUY",error_type="VALIDATION_ERROR"}

# Counterparty operations
trading_counterparties_created_total{type="INSTITUTIONAL"}

# Performance metrics
trading_trades_creation_time_seconds{instrument="AAPL"}
trading_trades_processing_time_seconds{operation="STATUS_UPDATE"}

# State gauges
trading_trades_active
trading_trades_pending
trading_counterparties_active
```

**System Metrics:**
- JVM memory, threads, GC
- HTTP request metrics
- Database connection pool metrics

### Grafana Dashboards

Ready-to-use Grafana configurations for:
- Trading volume and performance
- Error rates and types
- System resource utilization
- Business KPIs

### Sample Queries

```promql
# Trading volume per minute
rate(trading_trades_created_total[1m]) * 60

# Average trade processing time
rate(trading_trades_creation_time_seconds_sum[5m]) / rate(trading_trades_creation_time_seconds_count[5m])

# Error rate
rate(trading_trades_failed_total[5m]) / rate(trading_trades_created_total[5m])
```

## 🧪 Testing

### Test Categories

1. **Unit Tests**: Service and repository layer testing
2. **Integration Tests**: Full API testing with H2 database
3. **TestContainers Tests**: Real database integration testing
4. **Metrics Tests**: Monitoring and metrics validation

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test categories
./mvnw test -Dtest="*Test"                    # Unit tests
./mvnw test -Dtest="*ResourceTest"            # API integration tests
./mvnw test -Dtest="PostgreSQLTestContainersDemo"  # TestContainers demo
./mvnw test -Dtest="MetricsEndpointTest"      # Metrics tests

# Run tests with coverage
./mvnw test jacoco:report
```

### TestContainers Integration

The application includes comprehensive TestContainers support:

**PostgreSQL TestContainers:**
- Real PostgreSQL database for integration testing
- Automatic container lifecycle management
- Production-like testing environment

**Monitoring Stack TestContainers:**
- Prometheus container for metrics collection
- Grafana container for dashboard testing
- End-to-end monitoring validation

### Test Data Management

- **Builder Pattern**: Clean test data creation (no double brace initialization)
- **Unique Data Generation**: AtomicInteger counters prevent conflicts
- **Proper Cleanup**: Transactional test isolation

## 🛠️ Development

### Project Structure

```
src/
├── main/java/dev/mars/
│   ├── domain/              # Entity classes
│   ├── dto/                 # Data Transfer Objects
│   ├── exception/           # Custom exceptions
│   ├── lifecycle/           # Application lifecycle
│   ├── mapper/              # Entity-DTO mappers
│   ├── metrics/             # Custom metrics
│   ├── repository/          # Data access layer
│   ├── resource/            # REST endpoints
│   └── service/             # Business logic
├── test/java/dev/mars/
│   ├── metrics/             # Metrics testing
│   ├── repository/          # Repository tests
│   ├── resource/            # API tests
│   ├── service/             # Service tests
│   └── testcontainers/      # TestContainers infrastructure
└── test/resources/
    └── monitoring/          # Prometheus/Grafana configs
```

### Code Quality Standards

- **No Double Brace Initialization**: Uses builder pattern instead
- **Comprehensive Validation**: Input validation at all layers
- **Error Handling**: Global exception handling with proper HTTP status codes
- **Logging**: Structured logging with appropriate levels
- **Metrics**: Business and technical metrics throughout

### Development Workflow

1. **Start Development Mode**:
   ```bash
   ./mvnw quarkus:dev
   ```

2. **Access Dev UI**: http://localhost:8080/q/dev/
   - Database console
   - Health checks
   - Metrics viewer
   - Configuration editor

3. **Live Reload**: Changes are automatically reloaded

4. **Testing**: Run tests continuously during development

### Configuration Profiles

- **dev**: Development with PostgreSQL
- **test**: Testing with H2 in-memory database
- **prod**: Production configuration

## 🚀 Deployment

### Building for Production

```bash
# Standard JAR
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Uber JAR
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar

# Native executable
./mvnw package -Dnative
./target/augment-quarkus-1.0-SNAPSHOT-runner
```

### Docker Deployment

```dockerfile
FROM registry.access.redhat.com/ubi8/openjdk-21:1.18

COPY target/quarkus-app/lib/ /deployments/lib/
COPY target/quarkus-app/*.jar /deployments/
COPY target/quarkus-app/app/ /deployments/app/
COPY target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
```

### Environment Variables

```bash
# Database
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/trading
QUARKUS_DATASOURCE_USERNAME=trading_user
QUARKUS_DATASOURCE_PASSWORD=trading_password

# Monitoring
QUARKUS_MICROMETER_EXPORT_PROMETHEUS_ENABLED=true

# Logging
QUARKUS_LOG_LEVEL=INFO
```

## 🏗️ Architecture

### Technology Stack

- **Framework**: Quarkus 3.23.4
- **Database**: PostgreSQL (prod), H2 (test)
- **Monitoring**: Micrometer + Prometheus + Grafana
- **Testing**: JUnit 5 + TestContainers + RestAssured
- **Validation**: Hibernate Validator
- **Persistence**: Hibernate ORM with Panache

### Design Patterns

- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: API data transfer
- **Builder Pattern**: Clean object construction
- **Service Layer**: Business logic encapsulation
- **Global Exception Handling**: Centralized error management

### Key Dependencies

```xml
<!-- Core Quarkus -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>

<!-- Monitoring -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

## 📖 Additional Documentation

- [TestContainers Implementation](TESTCONTAINERS_IMPLEMENTATION.md)
- [Prometheus & Grafana Setup](PROMETHEUS_GRAFANA_IMPLEMENTATION.md)
- [API Examples and Testing Guide](api-examples.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Built with ❤️ using Quarkus - The Supersonic Subatomic Java Framework**

For more information about Quarkus, visit: https://quarkus.io/
