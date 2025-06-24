package dev.mars.service;

import dev.mars.domain.Counterparty;
import dev.mars.domain.Trade;
import dev.mars.dto.CreateTradeRequest;
import dev.mars.dto.TradeDto;
import dev.mars.exception.BusinessException;
import dev.mars.repository.CounterpartyRepository;
import dev.mars.repository.TradeRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TradeServiceTest {

    @Inject
    TradeService tradeService;

    @Inject
    TradeRepository tradeRepository;

    @Inject
    CounterpartyRepository counterpartyRepository;

    private Counterparty testCounterparty;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up before each test - delete trades first to avoid foreign key constraints
        tradeRepository.deleteAll();
        counterpartyRepository.deleteAll();

        // Create test counterparty
        testCounterparty = new Counterparty();
        testCounterparty.code = "TEST001";
        testCounterparty.name = "Test Counterparty";
        testCounterparty.email = "test@example.com";
        testCounterparty.type = Counterparty.CounterpartyType.CORPORATE;
        testCounterparty.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(testCounterparty);
    }

    @Test
    @Transactional
    void testCreateTrade() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";

        // When
        TradeDto result = tradeService.createTrade(request);

        // Then
        assertNotNull(result.id);
        assertEquals("TRD-001", result.tradeReference);
        assertEquals("AAPL", result.instrument);
        assertEquals(Trade.TradeType.BUY, result.tradeType);
        assertEquals(new BigDecimal("100"), result.quantity);
        assertEquals(new BigDecimal("150.00"), result.price);
        assertEquals("USD", result.currency);
        assertEquals(Trade.TradeStatus.PENDING, result.status);
        assertEquals(testCounterparty.id, result.counterpartyId);
    }

    @Test
    @Transactional
    void testCreateTradeWithDuplicateReference() {
        // Given
        CreateTradeRequest request1 = new CreateTradeRequest();
        request1.tradeReference = "DUPLICATE";
        request1.counterpartyId = testCounterparty.id;
        request1.instrument = "AAPL";
        request1.tradeType = Trade.TradeType.BUY;
        request1.quantity = new BigDecimal("100");
        request1.price = new BigDecimal("150.00");
        request1.tradeDate = LocalDate.now();
        request1.settlementDate = LocalDate.now().plusDays(2);
        request1.currency = "USD";

        CreateTradeRequest request2 = new CreateTradeRequest();
        request2.tradeReference = "DUPLICATE";
        request2.counterpartyId = testCounterparty.id;
        request2.instrument = "GOOGL";
        request2.tradeType = Trade.TradeType.SELL;
        request2.quantity = new BigDecimal("50");
        request2.price = new BigDecimal("2000.00");
        request2.tradeDate = LocalDate.now();
        request2.settlementDate = LocalDate.now().plusDays(2);
        request2.currency = "USD";

        // When
        tradeService.createTrade(request1);

        // Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tradeService.createTrade(request2);
        });
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @Transactional
    void testCreateTradeWithNonexistentCounterparty() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = 999L; // Non-existent
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tradeService.createTrade(request);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @Transactional
    void testCreateTradeWithInactiveCounterparty() {
        // Given - create a new inactive counterparty
        Counterparty inactiveCounterparty = new Counterparty();
        inactiveCounterparty.code = "INACTIVE001";
        inactiveCounterparty.name = "Inactive Counterparty";
        inactiveCounterparty.email = "inactive@example.com";
        inactiveCounterparty.type = Counterparty.CounterpartyType.CORPORATE;
        inactiveCounterparty.status = Counterparty.CounterpartyStatus.INACTIVE;
        counterpartyRepository.persist(inactiveCounterparty);

        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = inactiveCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tradeService.createTrade(request);
        });
        assertTrue(exception.getMessage().contains("inactive counterparty"));
    }

    @Test
    @Transactional
    void testCreateTradeWithInvalidSettlementDate() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().minusDays(1); // Before trade date
        request.currency = "USD";

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tradeService.createTrade(request);
        });
        assertTrue(exception.getMessage().contains("Settlement date cannot be before trade date"));
    }

    @Test
    @Transactional
    void testGetTradeById() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";
        TradeDto created = tradeService.createTrade(request);

        // When
        Optional<TradeDto> result = tradeService.getTradeById(created.id);

        // Then
        assertTrue(result.isPresent());
        assertEquals("TRD-001", result.get().tradeReference);
        assertEquals("AAPL", result.get().instrument);
    }

    @Test
    @Transactional
    void testGetTradeByIdNotFound() {
        // When
        Optional<TradeDto> result = tradeService.getTradeById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetTradeByReference() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "FINDME";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";
        tradeService.createTrade(request);

        // When
        Optional<TradeDto> result = tradeService.getTradeByReference("FINDME");

        // Then
        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().instrument);
    }

    @Test
    @Transactional
    void testGetAllTrades() {
        // Given
        CreateTradeRequest request1 = new CreateTradeRequest();
        request1.tradeReference = "TRD-001";
        request1.counterpartyId = testCounterparty.id;
        request1.instrument = "AAPL";
        request1.tradeType = Trade.TradeType.BUY;
        request1.quantity = new BigDecimal("100");
        request1.price = new BigDecimal("150.00");
        request1.tradeDate = LocalDate.now();
        request1.settlementDate = LocalDate.now().plusDays(2);
        request1.currency = "USD";
        tradeService.createTrade(request1);

        CreateTradeRequest request2 = new CreateTradeRequest();
        request2.tradeReference = "TRD-002";
        request2.counterpartyId = testCounterparty.id;
        request2.instrument = "GOOGL";
        request2.tradeType = Trade.TradeType.SELL;
        request2.quantity = new BigDecimal("50");
        request2.price = new BigDecimal("2000.00");
        request2.tradeDate = LocalDate.now();
        request2.settlementDate = LocalDate.now().plusDays(2);
        request2.currency = "USD";
        tradeService.createTrade(request2);

        // When
        List<TradeDto> result = tradeService.getAllTrades();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    @Transactional
    void testGetTradesByCounterpartyId() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";
        tradeService.createTrade(request);

        // When
        List<TradeDto> result = tradeService.getTradesByCounterpartyId(testCounterparty.id);

        // Then
        assertEquals(1, result.size());
        assertEquals("TRD-001", result.get(0).tradeReference);
    }

    @Test
    @Transactional
    void testGetTradesByStatus() {
        // Given
        CreateTradeRequest pendingRequest = new CreateTradeRequest();
        pendingRequest.tradeReference = "PENDING-001";
        pendingRequest.counterpartyId = testCounterparty.id;
        pendingRequest.instrument = "AAPL";
        pendingRequest.tradeType = Trade.TradeType.BUY;
        pendingRequest.quantity = new BigDecimal("100");
        pendingRequest.price = new BigDecimal("150.00");
        pendingRequest.tradeDate = LocalDate.now();
        pendingRequest.settlementDate = LocalDate.now().plusDays(2);
        pendingRequest.currency = "USD";
        pendingRequest.status = Trade.TradeStatus.PENDING;
        TradeDto pendingTrade = tradeService.createTrade(pendingRequest);

        CreateTradeRequest confirmedRequest = new CreateTradeRequest();
        confirmedRequest.tradeReference = "CONFIRMED-001";
        confirmedRequest.counterpartyId = testCounterparty.id;
        confirmedRequest.instrument = "GOOGL";
        confirmedRequest.tradeType = Trade.TradeType.SELL;
        confirmedRequest.quantity = new BigDecimal("50");
        confirmedRequest.price = new BigDecimal("2000.00");
        confirmedRequest.tradeDate = LocalDate.now();
        confirmedRequest.settlementDate = LocalDate.now().plusDays(2);
        confirmedRequest.currency = "USD";
        confirmedRequest.status = Trade.TradeStatus.PENDING;
        TradeDto confirmedTrade = tradeService.createTrade(confirmedRequest);

        // Update one to confirmed
        tradeService.updateTradeStatus(confirmedTrade.id, Trade.TradeStatus.CONFIRMED);

        // When
        List<TradeDto> pendingTrades = tradeService.getTradesByStatus(Trade.TradeStatus.PENDING);
        List<TradeDto> confirmedTrades = tradeService.getTradesByStatus(Trade.TradeStatus.CONFIRMED);

        // Then
        assertEquals(1, pendingTrades.size());
        assertEquals("PENDING-001", pendingTrades.get(0).tradeReference);
        assertEquals(1, confirmedTrades.size());
        assertEquals("CONFIRMED-001", confirmedTrades.get(0).tradeReference);
    }

    @Test
    @Transactional
    void testUpdateTradeStatus() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";
        TradeDto created = tradeService.createTrade(request);

        // When
        TradeDto updated = tradeService.updateTradeStatus(created.id, Trade.TradeStatus.CONFIRMED);

        // Then
        assertEquals(Trade.TradeStatus.CONFIRMED, updated.status);
    }

    @Test
    @Transactional
    void testDeleteTrade() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";
        TradeDto created = tradeService.createTrade(request);

        // When
        tradeService.deleteTrade(created.id);

        // Then
        Optional<TradeDto> result = tradeService.getTradeById(created.id);
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testDeleteConfirmedTrade() {
        // Given
        CreateTradeRequest request = new CreateTradeRequest();
        request.tradeReference = "TRD-001";
        request.counterpartyId = testCounterparty.id;
        request.instrument = "AAPL";
        request.tradeType = Trade.TradeType.BUY;
        request.quantity = new BigDecimal("100");
        request.price = new BigDecimal("150.00");
        request.tradeDate = LocalDate.now();
        request.settlementDate = LocalDate.now().plusDays(2);
        request.currency = "USD";
        TradeDto created = tradeService.createTrade(request);

        // Update to confirmed
        tradeService.updateTradeStatus(created.id, Trade.TradeStatus.CONFIRMED);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            tradeService.deleteTrade(created.id);
        });
        assertTrue(exception.getMessage().contains("Cannot delete confirmed or settled trades"));
    }

    @Test
    @Transactional
    void testGetTotalTradeValueByCounterparty() {
        // Given
        CreateTradeRequest request1 = new CreateTradeRequest();
        request1.tradeReference = "TRD-001";
        request1.counterpartyId = testCounterparty.id;
        request1.instrument = "AAPL";
        request1.tradeType = Trade.TradeType.BUY;
        request1.quantity = new BigDecimal("100");
        request1.price = new BigDecimal("150.00");
        request1.tradeDate = LocalDate.now();
        request1.settlementDate = LocalDate.now().plusDays(2);
        request1.currency = "USD";
        tradeService.createTrade(request1);

        CreateTradeRequest request2 = new CreateTradeRequest();
        request2.tradeReference = "TRD-002";
        request2.counterpartyId = testCounterparty.id;
        request2.instrument = "GOOGL";
        request2.tradeType = Trade.TradeType.SELL;
        request2.quantity = new BigDecimal("50");
        request2.price = new BigDecimal("2000.00");
        request2.tradeDate = LocalDate.now();
        request2.settlementDate = LocalDate.now().plusDays(2);
        request2.currency = "USD";
        tradeService.createTrade(request2);

        // When
        BigDecimal totalValue = tradeService.getTotalTradeValueByCounterparty(testCounterparty.id);

        // Then
        // Expected: (100 * 150) + (50 * 2000) = 15000 + 100000 = 115000
        assertEquals(0, new BigDecimal("115000.00").compareTo(totalValue));
    }
}
