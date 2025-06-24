package dev.mars.repository;

import dev.mars.domain.Counterparty;
import dev.mars.domain.Trade;
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
class TradeRepositoryTest {

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
    void testCreateAndFindTrade() {
        // Given
        Trade trade = createTestTrade("TRD-001", "AAPL");

        // When
        tradeRepository.persist(trade);

        // Then
        assertNotNull(trade.id);
        Optional<Trade> found = tradeRepository.findByIdOptional(trade.id);
        assertTrue(found.isPresent());
        assertEquals("TRD-001", found.get().tradeReference);
        assertEquals("AAPL", found.get().instrument);
    }

    @Test
    @Transactional
    void testFindByTradeReference() {
        // Given
        Trade trade = createTestTrade("UNIQUE-001", "GOOGL");
        tradeRepository.persist(trade);

        // When
        Optional<Trade> found = tradeRepository.findByTradeReference("UNIQUE-001");

        // Then
        assertTrue(found.isPresent());
        assertEquals("GOOGL", found.get().instrument);
    }

    @Test
    @Transactional
    void testFindByCounterpartyId() {
        // Given
        Trade trade1 = createTestTrade("TRD-001", "AAPL");
        Trade trade2 = createTestTrade("TRD-002", "GOOGL");
        tradeRepository.persist(trade1);
        tradeRepository.persist(trade2);

        // When
        List<Trade> trades = tradeRepository.findByCounterpartyId(testCounterparty.id);

        // Then
        assertEquals(2, trades.size());
    }

    @Test
    @Transactional
    void testFindByStatus() {
        // Given
        Trade pendingTrade = createTestTrade("TRD-001", "AAPL");
        pendingTrade.status = Trade.TradeStatus.PENDING;
        tradeRepository.persist(pendingTrade);

        Trade confirmedTrade = createTestTrade("TRD-002", "GOOGL");
        confirmedTrade.status = Trade.TradeStatus.CONFIRMED;
        tradeRepository.persist(confirmedTrade);

        // When
        List<Trade> pendingTrades = tradeRepository.findByStatus(Trade.TradeStatus.PENDING);
        List<Trade> confirmedTrades = tradeRepository.findByStatus(Trade.TradeStatus.CONFIRMED);

        // Then
        assertEquals(1, pendingTrades.size());
        assertEquals("TRD-001", pendingTrades.get(0).tradeReference);
        assertEquals(1, confirmedTrades.size());
        assertEquals("TRD-002", confirmedTrades.get(0).tradeReference);
    }

    @Test
    @Transactional
    void testFindByTradeType() {
        // Given
        Trade buyTrade = createTestTrade("TRD-001", "AAPL");
        buyTrade.tradeType = Trade.TradeType.BUY;
        tradeRepository.persist(buyTrade);

        Trade sellTrade = createTestTrade("TRD-002", "GOOGL");
        sellTrade.tradeType = Trade.TradeType.SELL;
        tradeRepository.persist(sellTrade);

        // When
        List<Trade> buyTrades = tradeRepository.findByTradeType(Trade.TradeType.BUY);
        List<Trade> sellTrades = tradeRepository.findByTradeType(Trade.TradeType.SELL);

        // Then
        assertEquals(1, buyTrades.size());
        assertEquals("TRD-001", buyTrades.get(0).tradeReference);
        assertEquals(1, sellTrades.size());
        assertEquals("TRD-002", sellTrades.get(0).tradeReference);
    }

    @Test
    @Transactional
    void testFindByInstrument() {
        // Given
        Trade appleTrade1 = createTestTrade("TRD-001", "AAPL");
        Trade appleTrade2 = createTestTrade("TRD-002", "AAPL");
        Trade googleTrade = createTestTrade("TRD-003", "GOOGL");
        tradeRepository.persist(appleTrade1);
        tradeRepository.persist(appleTrade2);
        tradeRepository.persist(googleTrade);

        // When
        List<Trade> appleTrades = tradeRepository.findByInstrument("AAPL");
        List<Trade> googleTrades = tradeRepository.findByInstrument("GOOGL");

        // Then
        assertEquals(2, appleTrades.size());
        assertEquals(1, googleTrades.size());
    }

    @Test
    @Transactional
    void testFindByTradeDateRange() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Trade todayTrade = createTestTrade("TRD-001", "AAPL");
        todayTrade.tradeDate = today;
        tradeRepository.persist(todayTrade);

        Trade yesterdayTrade = createTestTrade("TRD-002", "GOOGL");
        yesterdayTrade.tradeDate = yesterday;
        tradeRepository.persist(yesterdayTrade);

        Trade tomorrowTrade = createTestTrade("TRD-003", "MSFT");
        tomorrowTrade.tradeDate = tomorrow;
        tradeRepository.persist(tomorrowTrade);

        // When
        List<Trade> todayToTomorrowTrades = tradeRepository.findByTradeDateRange(today, tomorrow);
        List<Trade> yesterdayToTodayTrades = tradeRepository.findByTradeDateRange(yesterday, today);

        // Then
        assertEquals(2, todayToTomorrowTrades.size());
        assertEquals(2, yesterdayToTodayTrades.size());
    }

    @Test
    @Transactional
    void testFindPendingTrades() {
        // Given
        Trade pendingTrade = createTestTrade("TRD-001", "AAPL");
        pendingTrade.status = Trade.TradeStatus.PENDING;
        tradeRepository.persist(pendingTrade);

        Trade confirmedTrade = createTestTrade("TRD-002", "GOOGL");
        confirmedTrade.status = Trade.TradeStatus.CONFIRMED;
        tradeRepository.persist(confirmedTrade);

        // When
        List<Trade> pendingTrades = tradeRepository.findPendingTrades();

        // Then
        assertEquals(1, pendingTrades.size());
        assertEquals("TRD-001", pendingTrades.get(0).tradeReference);
    }

    @Test
    @Transactional
    void testExistsByTradeReference() {
        // Given
        Trade trade = createTestTrade("EXISTS-001", "AAPL");
        tradeRepository.persist(trade);

        // When & Then
        assertTrue(tradeRepository.existsByTradeReference("EXISTS-001"));
        assertFalse(tradeRepository.existsByTradeReference("NOTEXISTS"));
    }

    @Test
    @Transactional
    void testCountByStatus() {
        // Given
        Trade pending1 = createTestTrade("TRD-001", "AAPL");
        pending1.status = Trade.TradeStatus.PENDING;
        tradeRepository.persist(pending1);

        Trade pending2 = createTestTrade("TRD-002", "GOOGL");
        pending2.status = Trade.TradeStatus.PENDING;
        tradeRepository.persist(pending2);

        Trade confirmed = createTestTrade("TRD-003", "MSFT");
        confirmed.status = Trade.TradeStatus.CONFIRMED;
        tradeRepository.persist(confirmed);

        // When
        long pendingCount = tradeRepository.countByStatus(Trade.TradeStatus.PENDING);
        long confirmedCount = tradeRepository.countByStatus(Trade.TradeStatus.CONFIRMED);

        // Then
        assertEquals(2, pendingCount);
        assertEquals(1, confirmedCount);
    }

    @Test
    @Transactional
    void testGetTotalValueByCounterpartyId() {
        // Given
        Trade trade1 = createTestTrade("TRD-001", "AAPL");
        trade1.quantity = new BigDecimal("100");
        trade1.price = new BigDecimal("150.00");
        tradeRepository.persist(trade1);

        Trade trade2 = createTestTrade("TRD-002", "GOOGL");
        trade2.quantity = new BigDecimal("50");
        trade2.price = new BigDecimal("2000.00");
        tradeRepository.persist(trade2);

        // When
        BigDecimal totalValue = tradeRepository.getTotalValueByCounterpartyId(testCounterparty.id);

        // Then
        // Expected: (100 * 150) + (50 * 2000) = 15000 + 100000 = 115000
        assertEquals(0, new BigDecimal("115000.00").compareTo(totalValue));
    }

    private Trade createTestTrade(String reference, String instrument) {
        Trade trade = new Trade();
        trade.tradeReference = reference;
        trade.counterparty = testCounterparty;
        trade.instrument = instrument;
        trade.tradeType = Trade.TradeType.BUY;
        trade.quantity = new BigDecimal("100");
        trade.price = new BigDecimal("150.00");
        trade.tradeDate = LocalDate.now();
        trade.settlementDate = LocalDate.now().plusDays(2);
        trade.currency = "USD";
        trade.status = Trade.TradeStatus.PENDING;
        return trade;
    }
}
