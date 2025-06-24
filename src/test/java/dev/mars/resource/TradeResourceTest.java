package dev.mars.resource;

import dev.mars.domain.Counterparty;
import dev.mars.domain.Trade;
import dev.mars.dto.CreateCounterpartyRequest;
import dev.mars.dto.CreateTradeRequest;
import dev.mars.repository.CounterpartyRepository;
import dev.mars.repository.TradeRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TradeResource using H2 in-memory database.
 * Refactored to use builder pattern instead of double brace initialization.
 */
@QuarkusTest
class TradeResourceTest {

    @Inject
    TradeRepository tradeRepository;

    @Inject
    CounterpartyRepository counterpartyRepository;

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up before each test - delete trades first to avoid foreign key constraints
        tradeRepository.deleteAll();
        counterpartyRepository.deleteAll();
    }

    // Test Data Builders
    private static class CounterpartyRequestBuilder {
        private String name = "Test Bank";
        private String code = "TB" + COUNTER.getAndIncrement();
        private Counterparty.CounterpartyType type = Counterparty.CounterpartyType.INSTITUTIONAL;

        public static CounterpartyRequestBuilder builder() {
            return new CounterpartyRequestBuilder();
        }

        public CounterpartyRequestBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CounterpartyRequestBuilder code(String code) {
            this.code = code;
            return this;
        }

        public CounterpartyRequestBuilder type(Counterparty.CounterpartyType type) {
            this.type = type;
            return this;
        }

        public CreateCounterpartyRequest build() {
            CreateCounterpartyRequest request = new CreateCounterpartyRequest();
            request.name = this.name;
            request.code = this.code;
            request.type = this.type;
            return request;
        }
    }

    private static class TradeRequestBuilder {
        private String tradeReference = "TRD-" + COUNTER.getAndIncrement();
        private Long counterpartyId;
        private String instrument = "AAPL";
        private Trade.TradeType tradeType = Trade.TradeType.BUY;
        private BigDecimal quantity = new BigDecimal("100");
        private BigDecimal price = new BigDecimal("150.00");
        private LocalDate tradeDate = LocalDate.now();
        private LocalDate settlementDate = LocalDate.now().plusDays(2);
        private String currency = "USD";
        private Trade.TradeStatus status = Trade.TradeStatus.PENDING;
        private String notes;

        public static TradeRequestBuilder builder() {
            return new TradeRequestBuilder();
        }

        public TradeRequestBuilder tradeReference(String tradeReference) {
            this.tradeReference = tradeReference;
            return this;
        }

        public TradeRequestBuilder counterpartyId(Long counterpartyId) {
            this.counterpartyId = counterpartyId;
            return this;
        }

        public TradeRequestBuilder instrument(String instrument) {
            this.instrument = instrument;
            return this;
        }

        public TradeRequestBuilder tradeType(Trade.TradeType tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public TradeRequestBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public TradeRequestBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public TradeRequestBuilder tradeDate(LocalDate tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        public TradeRequestBuilder settlementDate(LocalDate settlementDate) {
            this.settlementDate = settlementDate;
            return this;
        }

        public TradeRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public TradeRequestBuilder status(Trade.TradeStatus status) {
            this.status = status;
            return this;
        }

        public TradeRequestBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public CreateTradeRequest build() {
            CreateTradeRequest request = new CreateTradeRequest();
            request.tradeReference = this.tradeReference;
            request.counterpartyId = this.counterpartyId;
            request.instrument = this.instrument;
            request.tradeType = this.tradeType;
            request.quantity = this.quantity;
            request.price = this.price;
            request.tradeDate = this.tradeDate;
            request.settlementDate = this.settlementDate;
            request.currency = this.currency;
            request.status = this.status;
            request.notes = this.notes;
            return request;
        }
    }

    private Long createTestCounterparty() {
        return createTestCounterparty(CounterpartyRequestBuilder.builder().build());
    }

    private Long createTestCounterparty(CreateCounterpartyRequest request) {
        Integer counterpartyIdInt = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        return counterpartyIdInt.longValue();
    }

    @Test
    void testGetAllTrades() {
        given()
                .when().get("/api/trades")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testCreateTrade() {
        Long counterpartyId = createTestCounterparty();

        CreateTradeRequest request = TradeRequestBuilder.builder()
                .tradeReference("TRD-001")
                .counterpartyId(counterpartyId)
                .instrument("AAPL")
                .tradeType(Trade.TradeType.BUY)
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("150.00"))
                .currency("USD")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/trades")
                .then()
                .statusCode(201)
                .body("tradeReference", equalTo("TRD-001"))
                .body("instrument", equalTo("AAPL"))
                .body("tradeType", equalTo("BUY"))
                .body("quantity", equalTo(100))
                .body("price", equalTo(150.00f))
                .body("currency", equalTo("USD"))
                .body("status", equalTo("PENDING"))
                .body("id", notNullValue());
    }

    @Test
    void testCreateTradeWithInvalidData() {
        CreateTradeRequest request = new CreateTradeRequest();
        // Missing required fields

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/trades")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateTradeWithNonexistentCounterparty() {
        CreateTradeRequest request = TradeRequestBuilder.builder()
                .tradeReference("TRD-NONEXISTENT")
                .counterpartyId(999L) // Non-existent
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/trades")
                .then()
                .statusCode(400)
                .body("message", containsString("not found"));
    }

    @Test
    void testCreateTradeWithDuplicateReference() {
        Long counterpartyId = createTestCounterparty();

        CreateTradeRequest request1 = TradeRequestBuilder.builder()
                .tradeReference("DUPLICATE")
                .counterpartyId(counterpartyId)
                .instrument("AAPL")
                .tradeType(Trade.TradeType.BUY)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request1)
                .when().post("/api/trades")
                .then()
                .statusCode(201);

        // Try to create second with same reference
        CreateTradeRequest request2 = TradeRequestBuilder.builder()
                .tradeReference("DUPLICATE")
                .counterpartyId(counterpartyId)
                .instrument("GOOGL")
                .tradeType(Trade.TradeType.SELL)
                .quantity(new BigDecimal("50"))
                .price(new BigDecimal("2000.00"))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request2)
                .when().post("/api/trades")
                .then()
                .statusCode(400)
                .body("message", containsString("already exists"));
    }

    @Test
    void testGetTradeById() {
        Long counterpartyId = createTestCounterparty();

        CreateTradeRequest tradeRequest = TradeRequestBuilder.builder()
                .tradeReference("TRD-GET-BY-ID")
                .counterpartyId(counterpartyId)
                .instrument("AAPL")
                .tradeType(Trade.TradeType.BUY)
                .build();

        Integer tradeIdInt = given()
                .contentType(ContentType.JSON)
                .body(tradeRequest)
                .when().post("/api/trades")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long tradeId = tradeIdInt.longValue();

        given()
                .when().get("/api/trades/{id}", tradeId)
                .then()
                .statusCode(200)
                .body("tradeReference", equalTo("TRD-GET-BY-ID"))
                .body("instrument", equalTo("AAPL"));
    }

    @Test
    void testGetTradeByIdNotFound() {
        given()
                .when().get("/api/trades/{id}", 999L)
                .then()
                .statusCode(404);
    }

    @Test
    void testGetTradeByReference() {
        Long counterpartyId = createTestCounterparty();

        CreateTradeRequest tradeRequest = TradeRequestBuilder.builder()
                .tradeReference("FINDME")
                .counterpartyId(counterpartyId)
                .instrument("AAPL")
                .tradeType(Trade.TradeType.BUY)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(tradeRequest)
                .when().post("/api/trades")
                .then()
                .statusCode(201);

        given()
                .when().get("/api/trades/reference/{reference}", "FINDME")
                .then()
                .statusCode(200)
                .body("tradeReference", equalTo("FINDME"))
                .body("instrument", equalTo("AAPL"));
    }

    @Test
    void testGetTradeByReferenceNotFound() {
        given()
                .when().get("/api/trades/reference/{reference}", "NOTFOUND")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetPendingTrades() {
        Long counterpartyId = createTestCounterparty();

        // Create pending trade
        CreateTradeRequest pendingTradeRequest = TradeRequestBuilder.builder()
                .tradeReference("PENDING-001")
                .counterpartyId(counterpartyId)
                .instrument("AAPL")
                .tradeType(Trade.TradeType.BUY)
                .status(Trade.TradeStatus.PENDING)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(pendingTradeRequest)
                .when().post("/api/trades")
                .then()
                .statusCode(201);

        // Create trade that will be confirmed
        CreateTradeRequest confirmedTradeRequest = TradeRequestBuilder.builder()
                .tradeReference("CONFIRMED-001")
                .counterpartyId(counterpartyId)
                .instrument("GOOGL")
                .tradeType(Trade.TradeType.SELL)
                .quantity(new BigDecimal("50"))
                .price(new BigDecimal("2000.00"))
                .status(Trade.TradeStatus.PENDING)
                .build();

        Integer confirmedTradeIdInt = given()
                .contentType(ContentType.JSON)
                .body(confirmedTradeRequest)
                .when().post("/api/trades")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long confirmedTradeId = confirmedTradeIdInt.longValue();

        // Update to confirmed
        given()
                .queryParam("status", "CONFIRMED")
                .when().patch("/api/trades/{id}/status", confirmedTradeId)
                .then()
                .statusCode(200);

        given()
                .when().get("/api/trades/pending")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].tradeReference", equalTo("PENDING-001"));
    }
}
