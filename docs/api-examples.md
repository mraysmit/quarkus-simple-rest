# API Examples and Testing Guide

This document provides comprehensive examples for using the Trading Application API.

## 🚀 Getting Started

Start the application in development mode:
```bash
./mvnw quarkus:dev
```

The API will be available at: `http://localhost:8080`

## 👥 Counterparty Management

### Create Counterparty

```bash
curl -X POST http://localhost:8080/api/counterparties \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Goldman Sachs",
    "code": "GS001",
    "email": "trading@gs.com",
    "type": "INSTITUTIONAL"
  }'
```

**Response:**
```json
{
  "id": 1,
  "name": "Goldman Sachs",
  "code": "GS001",
  "email": "trading@gs.com",
  "type": "INSTITUTIONAL",
  "status": "ACTIVE",
  "createdAt": "2023-12-01T10:00:00Z",
  "updatedAt": "2023-12-01T10:00:00Z"
}
```

### List All Counterparties

```bash
curl http://localhost:8080/api/counterparties
```

### Get Counterparty by ID

```bash
curl http://localhost:8080/api/counterparties/1
```

### Update Counterparty

```bash
curl -X PUT http://localhost:8080/api/counterparties/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Goldman Sachs Group",
    "code": "GS001",
    "email": "trading@gs.com",
    "type": "INSTITUTIONAL",
    "status": "ACTIVE"
  }'
```

### Get Counterparty Statistics

```bash
curl http://localhost:8080/api/counterparties/stats/count
```

**Response:**
```json
{
  "totalCount": 5,
  "activeCount": 4,
  "inactiveCount": 1
}
```

## 📈 Trade Management

### Create Trade

```bash
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "tradeReference": "TRD-BUY-001",
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

**Response:**
```json
{
  "id": 1,
  "tradeReference": "TRD-BUY-001",
  "counterparty": {
    "id": 1,
    "name": "Goldman Sachs",
    "code": "GS001"
  },
  "instrument": "AAPL",
  "tradeType": "BUY",
  "quantity": 100.00,
  "price": 150.00,
  "tradeDate": "2023-12-01",
  "settlementDate": "2023-12-03",
  "currency": "USD",
  "status": "PENDING",
  "createdAt": "2023-12-01T10:00:00Z",
  "updatedAt": "2023-12-01T10:00:00Z"
}
```

### List All Trades (with pagination)

```bash
# Default pagination (page 0, size 20)
curl http://localhost:8080/api/trades

# Custom pagination
curl "http://localhost:8080/api/trades?page=0&size=10"

# Filter by counterparty
curl "http://localhost:8080/api/trades?counterpartyId=1"

# Filter by status
curl "http://localhost:8080/api/trades?status=PENDING"
```

### Update Trade Status

```bash
curl -X PATCH "http://localhost:8080/api/trades/1/status?status=CONFIRMED"
```

### Get Pending Trades

```bash
curl http://localhost:8080/api/trades/pending
```

### Get Trade by Reference

```bash
curl http://localhost:8080/api/trades/reference/TRD-BUY-001
```

## 🏥 Health Checks

### Application Health

```bash
curl http://localhost:8080/q/health
```

**Response:**
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Database connections health check",
      "status": "UP"
    }
  ]
}
```

### Liveness Probe

```bash
curl http://localhost:8080/q/health/live
```

### Readiness Probe

```bash
curl http://localhost:8080/q/health/ready
```

## 📊 Metrics

### Prometheus Metrics

```bash
curl http://localhost:8080/q/metrics
```

This returns metrics in OpenMetrics format, including:
- Custom trading metrics
- JVM metrics
- HTTP request metrics
- Database metrics

### Sample Metrics Output

```
# HELP trading_trades_created_total Total number of trades created
# TYPE trading_trades_created_total counter
trading_trades_created_total{instrument="AAPL",type="BUY"} 5.0

# HELP trading_trades_active Number of currently active trades
# TYPE trading_trades_active gauge
trading_trades_active 3.0

# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 1.048576E7
```

## 🧪 Testing Scenarios

### Complete Trading Workflow

1. **Create Counterparty:**
```bash
COUNTERPARTY_ID=$(curl -s -X POST http://localhost:8080/api/counterparties \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Bank",
    "code": "TB001",
    "email": "test@bank.com",
    "type": "INSTITUTIONAL"
  }' | jq -r '.id')
```

2. **Create Trade:**
```bash
TRADE_ID=$(curl -s -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d "{
    \"tradeReference\": \"TRD-$(date +%s)\",
    \"counterpartyId\": $COUNTERPARTY_ID,
    \"instrument\": \"GOOGL\",
    \"tradeType\": \"BUY\",
    \"quantity\": 50,
    \"price\": 2800.75,
    \"tradeDate\": \"$(date +%Y-%m-%d)\",
    \"settlementDate\": \"$(date -d '+2 days' +%Y-%m-%d)\",
    \"currency\": \"USD\"
  }" | jq -r '.id')
```

3. **Confirm Trade:**
```bash
curl -X PATCH "http://localhost:8080/api/trades/$TRADE_ID/status?status=CONFIRMED"
```

4. **Settle Trade:**
```bash
curl -X PATCH "http://localhost:8080/api/trades/$TRADE_ID/status?status=SETTLED"
```

### Error Handling Examples

**Invalid Counterparty:**
```bash
curl -X POST http://localhost:8080/api/counterparties \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "code": "INVALID",
    "type": "UNKNOWN"
  }'
```

**Duplicate Trade Reference:**
```bash
curl -X POST http://localhost:8080/api/trades \
  -H "Content-Type: application/json" \
  -d '{
    "tradeReference": "TRD-BUY-001",
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

## 🔧 Development Tools

### Using HTTPie (Alternative to curl)

```bash
# Install HTTPie
pip install httpie

# Create counterparty
http POST localhost:8080/api/counterparties \
  name="JPMorgan Chase" \
  code="JPM001" \
  email="trading@jpmorgan.com" \
  type="INSTITUTIONAL"

# Create trade
http POST localhost:8080/api/trades \
  tradeReference="TRD-SELL-001" \
  counterpartyId:=1 \
  instrument="MSFT" \
  tradeType="SELL" \
  quantity:=200 \
  price:=300.50 \
  tradeDate="2023-12-01" \
  settlementDate="2023-12-03" \
  currency="USD"
```

### Using Postman

Import the following collection for comprehensive API testing:

```json
{
  "info": {
    "name": "Trading Application API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    }
  ]
}
```

## 📝 Notes

- All timestamps are in ISO 8601 format
- Monetary values use decimal precision
- Trade references must be unique
- Counterparty codes must be unique
- Settlement date must be after trade date
- All endpoints return appropriate HTTP status codes
- Error responses include detailed error messages

For more information, see the main [README.md](README.md) file.
