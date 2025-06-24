package dev.mars.testcontainers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration of TestContainers with PostgreSQL.
 * This test shows how TestContainers can be used for database integration testing
 * with a real PostgreSQL instance instead of H2 in-memory database.
 * 
 * This is a standalone test that demonstrates TestContainers functionality
 * without requiring Quarkus configuration changes.
 */
@Testcontainers
@DisplayName("PostgreSQL TestContainers Demonstration")
class PostgreSQLTestContainersDemo {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("trading_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true); // Reuse container for better performance

    @Test
    @DisplayName("PostgreSQL container should start and be accessible")
    void testPostgreSQLContainerStartup() {
        // Verify container is running
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        
        // Verify container configuration
        assertEquals("trading_test", postgres.getDatabaseName());
        assertEquals("test_user", postgres.getUsername());
        assertEquals("test_password", postgres.getPassword());
        
        // Verify JDBC URL format
        String jdbcUrl = postgres.getJdbcUrl();
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"));
        assertTrue(jdbcUrl.contains("trading_test"));
        
        // Verify port mapping
        assertTrue(postgres.getFirstMappedPort() > 0);
        
        System.out.println("PostgreSQL Container Details:");
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("Host: " + postgres.getHost());
        System.out.println("Port: " + postgres.getFirstMappedPort());
        System.out.println("Database: " + postgres.getDatabaseName());
    }

    @Test
    @DisplayName("Should be able to connect and execute SQL queries")
    void testDatabaseConnection() throws SQLException {
        // Test direct JDBC connection to PostgreSQL container
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            
            // Test basic SQL operations
            try (Statement statement = connection.createStatement()) {
                // Create a test table
                statement.execute("""
                    CREATE TABLE test_trades (
                        id SERIAL PRIMARY KEY,
                        trade_reference VARCHAR(50) UNIQUE NOT NULL,
                        instrument VARCHAR(10) NOT NULL,
                        quantity DECIMAL(15,2) NOT NULL,
                        price DECIMAL(15,2) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
                
                // Insert test data
                try (PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO test_trades (trade_reference, instrument, quantity, price) VALUES (?, ?, ?, ?)")) {
                    
                    insertStmt.setString(1, "TRD-001");
                    insertStmt.setString(2, "AAPL");
                    insertStmt.setBigDecimal(3, new java.math.BigDecimal("100"));
                    insertStmt.setBigDecimal(4, new java.math.BigDecimal("175.50"));
                    int rowsInserted = insertStmt.executeUpdate();
                    assertEquals(1, rowsInserted);
                    
                    insertStmt.setString(1, "TRD-002");
                    insertStmt.setString(2, "GOOGL");
                    insertStmt.setBigDecimal(3, new java.math.BigDecimal("50"));
                    insertStmt.setBigDecimal(4, new java.math.BigDecimal("2800.75"));
                    rowsInserted = insertStmt.executeUpdate();
                    assertEquals(1, rowsInserted);
                }
                
                // Query and verify data
                try (ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_trades")) {
                    assertTrue(resultSet.next());
                    assertEquals(2, resultSet.getInt(1));
                }
                
                // Test PostgreSQL-specific features
                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT trade_reference, instrument, quantity * price as total_value FROM test_trades ORDER BY total_value DESC")) {
                    
                    assertTrue(resultSet.next());
                    assertEquals("TRD-002", resultSet.getString("trade_reference"));
                    assertEquals("GOOGL", resultSet.getString("instrument"));
                    assertEquals(0, new java.math.BigDecimal("140037.50").compareTo(resultSet.getBigDecimal("total_value")));

                    assertTrue(resultSet.next());
                    assertEquals("TRD-001", resultSet.getString("trade_reference"));
                    assertEquals("AAPL", resultSet.getString("instrument"));
                    assertEquals(0, new java.math.BigDecimal("17550.00").compareTo(resultSet.getBigDecimal("total_value")));
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle PostgreSQL constraints and transactions")
    void testPostgreSQLConstraintsAndTransactions() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            // Create table with constraints
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS test_counterparties (
                        id SERIAL PRIMARY KEY,
                        code VARCHAR(10) UNIQUE NOT NULL,
                        name VARCHAR(100) NOT NULL,
                        status VARCHAR(20) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }
            
            // Test transaction rollback
            connection.setAutoCommit(false);
            
            try (PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO test_counterparties (code, name) VALUES (?, ?)")) {
                
                // Insert valid data
                insertStmt.setString(1, "CP001");
                insertStmt.setString(2, "Test Counterparty 1");
                insertStmt.executeUpdate();
                
                // Try to insert duplicate code (should fail)
                insertStmt.setString(1, "CP001");
                insertStmt.setString(2, "Test Counterparty 2");
                
                assertThrows(SQLException.class, insertStmt::executeUpdate);
                
                // Rollback transaction
                connection.rollback();
            }
            
            // Verify no data was inserted due to rollback
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_counterparties")) {
                assertTrue(resultSet.next());
                assertEquals(0, resultSet.getInt(1));
            }
            
            connection.setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("Demonstrate TestContainers benefits over H2")
    void testPostgreSQLSpecificFeatures() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword())) {
            
            try (Statement statement = connection.createStatement()) {
                // Test PostgreSQL-specific data types and functions
                statement.execute("""
                    CREATE TABLE test_advanced_features (
                        id SERIAL PRIMARY KEY,
                        data JSONB,
                        tags TEXT[],
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
                    )
                """);
                
                // Insert data with PostgreSQL-specific types
                try (PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO test_advanced_features (data, tags) VALUES (?::jsonb, ?)")) {
                    
                    insertStmt.setString(1, "{\"trade_id\": \"TRD-001\", \"amount\": 1000.50}");
                    insertStmt.setArray(2, connection.createArrayOf("TEXT", new String[]{"urgent", "high-value"}));
                    insertStmt.executeUpdate();
                }
                
                // Query using PostgreSQL-specific JSON operations
                try (ResultSet resultSet = statement.executeQuery(
                        "SELECT data->>'trade_id' as trade_id, array_length(tags, 1) as tag_count FROM test_advanced_features")) {
                    
                    assertTrue(resultSet.next());
                    assertEquals("TRD-001", resultSet.getString("trade_id"));
                    assertEquals(2, resultSet.getInt("tag_count"));
                }
                
                System.out.println("✅ PostgreSQL-specific features working correctly!");
                System.out.println("   - JSONB data type");
                System.out.println("   - Array data type");
                System.out.println("   - JSON operators");
                System.out.println("   - Timezone-aware timestamps");
            }
        }
    }
}
