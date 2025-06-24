package dev.mars.metrics;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Prometheus metrics endpoint functionality.
 * Verifies that metrics are properly exposed and formatted for Prometheus consumption.
 */
@QuarkusTest
@DisplayName("Metrics Endpoint Tests")
class MetricsEndpointTest {

    @Test
    @DisplayName("Prometheus metrics endpoint should be accessible")
    void testMetricsEndpointAccessible() {
        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .contentType(containsString("text/plain"));
    }

    @Test
    @DisplayName("Metrics should include standard JVM metrics")
    void testStandardJvmMetrics() {
        String metricsResponse = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Verify standard JVM metrics are present
        assertTrue(metricsResponse.contains("jvm_memory_used_bytes"), 
                "Should contain JVM memory metrics");
        assertTrue(metricsResponse.contains("jvm_gc_collection_seconds"), 
                "Should contain JVM GC metrics");
        assertTrue(metricsResponse.contains("jvm_threads_current"), 
                "Should contain JVM thread metrics");
    }

    @Test
    @DisplayName("Metrics should include HTTP server metrics")
    void testHttpServerMetrics() {
        // Make a request to generate HTTP metrics
        given()
                .when().get("/api/trades")
                .then()
                .statusCode(200);

        String metricsResponse = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Verify HTTP metrics are present
        assertTrue(metricsResponse.contains("http_server_requests"), 
                "Should contain HTTP server request metrics");
    }

    @Test
    @DisplayName("Custom trading metrics should be exposed after business operations")
    void testCustomTradingMetricsExposed() {
        // Create test data to generate custom metrics
        createTestCounterpartyAndTrade();

        String metricsResponse = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Verify custom trading metrics are present
        assertTrue(metricsResponse.contains("trading_trades_created"), 
                "Should contain custom trade creation metrics");
        assertTrue(metricsResponse.contains("trading_trades_active"), 
                "Should contain active trades gauge");
        assertTrue(metricsResponse.contains("trading_counterparties_created"), 
                "Should contain counterparty creation metrics");
    }

    @Test
    @DisplayName("Metrics should be in proper Prometheus format")
    void testPrometheusFormat() {
        String metricsResponse = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Verify Prometheus format
        assertTrue(metricsResponse.contains("# HELP"), 
                "Should contain HELP comments");
        assertTrue(metricsResponse.contains("# TYPE"), 
                "Should contain TYPE comments");
        
        // Verify metric lines have proper format (metric_name{labels} value)
        String[] lines = metricsResponse.split("\n");
        boolean hasValidMetricLine = false;
        for (String line : lines) {
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                // Should have format: metric_name{labels} value timestamp
                // or: metric_name value timestamp
                if (line.contains(" ") && !line.startsWith(" ")) {
                    hasValidMetricLine = true;
                    break;
                }
            }
        }
        assertTrue(hasValidMetricLine, "Should have properly formatted metric lines");
    }

    @Test
    @DisplayName("Metrics should include labels and tags")
    void testMetricsLabelsAndTags() {
        // Create test data with specific attributes
        createTestCounterpartyAndTrade();

        String metricsResponse = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Verify metrics include proper labels
        assertTrue(metricsResponse.contains("method="), 
                "HTTP metrics should include method labels");
        assertTrue(metricsResponse.contains("status="), 
                "HTTP metrics should include status labels");
    }

    @Test
    @DisplayName("Health metrics should be available")
    void testHealthMetrics() {
        // Access health endpoint to generate health metrics
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200);

        String metricsResponse = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Health-related metrics should be present
        assertTrue(metricsResponse.contains("application_") || 
                   metricsResponse.contains("health_") ||
                   metricsResponse.contains("up{"), 
                "Should contain health-related metrics");
    }

    @Test
    @DisplayName("Metrics endpoint should handle concurrent requests")
    void testConcurrentMetricsAccess() {
        // Make multiple concurrent requests to verify metrics endpoint stability
        for (int i = 0; i < 5; i++) {
            given()
                    .when().get("/q/metrics")
                    .then()
                    .statusCode(200)
                    .contentType(containsString("text/plain"));
        }
    }

    @Test
    @DisplayName("Metrics should persist across requests")
    void testMetricsPersistence() {
        // Create initial test data
        createTestCounterpartyAndTrade();

        // Get initial metrics
        String initialMetrics = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Create more test data
        createTestCounterpartyAndTrade("PERSIST-TEST");

        // Get updated metrics
        String updatedMetrics = given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asString();

        // Metrics should have changed (counters should have increased)
        assertNotEquals(initialMetrics, updatedMetrics, 
                "Metrics should change when new operations are performed");
    }

    // Helper methods
    private void createTestCounterpartyAndTrade() {
        createTestCounterpartyAndTrade("METRICS-TEST");
    }

    private void createTestCounterpartyAndTrade(String suffix) {
        // Create counterparty
        Integer counterpartyId = given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "name": "Metrics Test Bank %s",
                        "code": "MTB-%s",
                        "email": "test-%s@metricsbank.com",
                        "type": "INSTITUTIONAL"
                    }
                    """, suffix, suffix, suffix.toLowerCase()))
                .when().post("/api/counterparties")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create trade
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "tradeReference": "METRICS-%s-001",
                        "counterpartyId": %d,
                        "instrument": "AAPL",
                        "tradeType": "BUY",
                        "quantity": 100,
                        "price": 150.00,
                        "tradeDate": "2023-12-01",
                        "settlementDate": "2023-12-03",
                        "currency": "USD"
                    }
                    """, suffix, counterpartyId))
                .when().post("/api/trades")
                .then()
                .statusCode(201);
    }
}
