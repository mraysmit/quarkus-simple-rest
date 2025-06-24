package dev.mars.resource;

import dev.mars.domain.Counterparty;
import dev.mars.dto.CreateCounterpartyRequest;
import dev.mars.repository.CounterpartyRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CounterpartyResourceTest {

    @Inject
    CounterpartyRepository counterpartyRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up before each test - delete trades first to avoid foreign key constraints
        counterpartyRepository.getEntityManager().createQuery("DELETE FROM Trade").executeUpdate();
        counterpartyRepository.deleteAll();
    }

    // Test Data Builder for CounterpartyRequest
    private static class CounterpartyRequestBuilder {
        private String name = "Test Bank";
        private String code = "TB001";
        private String email;
        private Counterparty.CounterpartyType type = Counterparty.CounterpartyType.CORPORATE;
        private Counterparty.CounterpartyStatus status = Counterparty.CounterpartyStatus.ACTIVE;

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

        public CounterpartyRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public CounterpartyRequestBuilder type(Counterparty.CounterpartyType type) {
            this.type = type;
            return this;
        }

        public CounterpartyRequestBuilder status(Counterparty.CounterpartyStatus status) {
            this.status = status;
            return this;
        }

        public CreateCounterpartyRequest build() {
            CreateCounterpartyRequest request = new CreateCounterpartyRequest();
            request.name = this.name;
            request.code = this.code;
            request.email = this.email != null ? this.email : this.code.toLowerCase() + "@test.com";
            request.type = this.type;
            request.status = this.status;
            return request;
        }
    }

    @Test
    void testGetAllCounterparties() {
        given()
                .when().get("/api/counterparties")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @Transactional
    void testCreateCounterparty() {
        CreateCounterpartyRequest request = CounterpartyRequestBuilder.builder()
                .name("Test Bank")
                .code("TB001")
                .email("test@bank.com")
                .type(Counterparty.CounterpartyType.INSTITUTIONAL)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .body("name", equalTo("Test Bank"))
                .body("code", equalTo("TB001"))
                .body("email", equalTo("test@bank.com"))
                .body("type", equalTo("INSTITUTIONAL"))
                .body("status", equalTo("ACTIVE"))
                .body("id", notNullValue());
    }

    @Test
    void testCreateCounterpartyWithInvalidData() {
        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        // Missing required fields

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/api/counterparties")
                .then()
                .statusCode(400);
    }

    @Test
    @Transactional
    void testCreateCounterpartyWithDuplicateCode() {
        // Create first counterparty
        CreateCounterpartyRequest request1 = CounterpartyRequestBuilder.builder()
                .name("Bank One")
                .code("DUPLICATE")
                .type(Counterparty.CounterpartyType.INSTITUTIONAL)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request1)
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        // Try to create second with same code
        CreateCounterpartyRequest request2 = CounterpartyRequestBuilder.builder()
                .name("Bank Two")
                .code("DUPLICATE")
                .type(Counterparty.CounterpartyType.CORPORATE)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request2)
                .when().post("/api/counterparties")
                .then()
                .statusCode(400)
                .body("message", containsString("already exists"));
    }

    @Test
    void testGetCounterpartyById() {
        // Create a counterparty first
        Counterparty counterparty = createTestCounterparty("TEST001", "Test Bank");

        Integer counterpartyIdInt = given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Test Bank";
                    code = "TEST001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long counterpartyId = counterpartyIdInt.longValue();

        given()
                .when().get("/api/counterparties/{id}", counterpartyId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Bank"))
                .body("code", equalTo("TEST001"));
    }

    @Test
    void testGetCounterpartyByIdNotFound() {
        given()
                .when().get("/api/counterparties/{id}", 999L)
                .then()
                .statusCode(404);
    }

    @Test
    void testGetCounterpartyByCode() {
        // Create a counterparty first
        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Find Me Bank";
                    code = "FINDME";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        given()
                .when().get("/api/counterparties/code/{code}", "FINDME")
                .then()
                .statusCode(200)
                .body("name", equalTo("Find Me Bank"))
                .body("code", equalTo("FINDME"));
    }

    @Test
    void testGetCounterpartyByCodeNotFound() {
        given()
                .when().get("/api/counterparties/code/{code}", "NOTFOUND")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetActiveCounterparties() {
        // Create active counterparty
        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Active Bank";
                    code = "ACTIVE001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                    status = Counterparty.CounterpartyStatus.ACTIVE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        // Create inactive counterparty
        Integer inactiveIdInt = given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Inactive Bank";
                    code = "INACTIVE001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                    status = Counterparty.CounterpartyStatus.ACTIVE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long inactiveId = inactiveIdInt.longValue();

        // Update to inactive
        given()
                .queryParam("status", "INACTIVE")
                .when().patch("/api/counterparties/{id}/status", inactiveId)
                .then()
                .statusCode(200);

        given()
                .when().get("/api/counterparties/active")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].code", equalTo("ACTIVE001"));
    }

    @Test
    void testUpdateCounterparty() {
        // Create a counterparty first
        Integer counterpartyIdInt = given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Original Name";
                    code = "UPDATE001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long counterpartyId = counterpartyIdInt.longValue();

        CreateCounterpartyRequest updateRequest = new CreateCounterpartyRequest();
        updateRequest.name = "Updated Name";
        updateRequest.code = "UPDATED001";
        updateRequest.email = "updated@email.com";
        updateRequest.type = Counterparty.CounterpartyType.INSTITUTIONAL;

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when().put("/api/counterparties/{id}", counterpartyId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Name"))
                .body("code", equalTo("UPDATED001"))
                .body("email", equalTo("updated@email.com"));
    }

    @Test
    void testUpdateCounterpartyNotFound() {
        CreateCounterpartyRequest updateRequest = new CreateCounterpartyRequest();
        updateRequest.name = "Updated Name";
        updateRequest.code = "UPDATED001";
        updateRequest.type = Counterparty.CounterpartyType.INSTITUTIONAL;

        given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when().put("/api/counterparties/{id}", 999L)
                .then()
                .statusCode(400)
                .body("message", containsString("not found"));
    }

    @Test
    void testUpdateCounterpartyStatus() {
        // Create a counterparty first
        Integer counterpartyIdInt = given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Status Test Bank";
                    code = "STATUS001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long counterpartyId = counterpartyIdInt.longValue();

        given()
                .queryParam("status", "SUSPENDED")
                .when().patch("/api/counterparties/{id}/status", counterpartyId)
                .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));
    }

    @Test
    void testDeleteCounterparty() {
        // Create a counterparty first
        Integer counterpartyIdInt = given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "To Delete Bank";
                    code = "DELETE001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long counterpartyId = counterpartyIdInt.longValue();

        given()
                .when().delete("/api/counterparties/{id}", counterpartyId)
                .then()
                .statusCode(204);

        // Verify it's deleted
        given()
                .when().get("/api/counterparties/{id}", counterpartyId)
                .then()
                .statusCode(404);
    }

    @Test
    void testDeleteCounterpartyNotFound() {
        given()
                .when().delete("/api/counterparties/{id}", 999L)
                .then()
                .statusCode(400)
                .body("message", containsString("not found"));
    }

    @Test
    void testGetCounterpartyStats() {
        // Create some counterparties
        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Active Bank 1";
                    code = "ACTIVE001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                    status = Counterparty.CounterpartyStatus.ACTIVE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Active Bank 2";
                    code = "ACTIVE002";
                    type = Counterparty.CounterpartyType.CORPORATE;
                    status = Counterparty.CounterpartyStatus.ACTIVE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        Integer inactiveIdInt = given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Inactive Bank";
                    code = "INACTIVE001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                    status = Counterparty.CounterpartyStatus.ACTIVE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");
        Long inactiveId = inactiveIdInt.longValue();

        // Update to inactive
        given()
                .queryParam("status", "INACTIVE")
                .when().patch("/api/counterparties/{id}/status", inactiveId)
                .then()
                .statusCode(200);

        given()
                .when().get("/api/counterparties/stats/count")
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(3))
                .body("activeCount", equalTo(2));
    }

    @Test
    void testSearchCounterpartiesByName() {
        // Create test data
        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Global Investment Bank";
                    code = "BANK001";
                    type = Counterparty.CounterpartyType.INSTITUTIONAL;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Local Community Bank";
                    code = "BANK002";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(new CreateCounterpartyRequest() {{
                    name = "Tech Corporation";
                    code = "CORP001";
                    type = Counterparty.CounterpartyType.CORPORATE;
                }})
                .when().post("/api/counterparties")
                .then()
                .statusCode(201);

        given()
                .queryParam("search", "Bank")
                .when().get("/api/counterparties")
                .then()
                .statusCode(200)
                .body("size()", is(2));

        given()
                .queryParam("search", "Global")
                .when().get("/api/counterparties")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].code", equalTo("BANK001"));
    }

    private Counterparty createTestCounterparty(String code, String name) {
        Counterparty counterparty = new Counterparty();
        counterparty.code = code;
        counterparty.name = name;
        counterparty.email = code.toLowerCase() + "@test.com";
        counterparty.type = Counterparty.CounterpartyType.CORPORATE;
        counterparty.status = Counterparty.CounterpartyStatus.ACTIVE;
        return counterparty;
    }
}
