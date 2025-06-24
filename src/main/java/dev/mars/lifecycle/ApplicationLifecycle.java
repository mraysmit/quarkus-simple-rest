package dev.mars.lifecycle;

import dev.mars.domain.Counterparty;
import dev.mars.domain.Trade;
import dev.mars.repository.CounterpartyRepository;
import dev.mars.repository.TradeRepository;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;

@ApplicationScoped
public class ApplicationLifecycle {

    private static final Logger LOG = Logger.getLogger(ApplicationLifecycle.class);

    @Inject
    CounterpartyRepository counterpartyRepository;

    @Inject
    TradeRepository tradeRepository;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOG.info("=== Quarkus Trading Application Starting ===");
        
        // Initialize sample data if database is empty
        if (counterpartyRepository.count() == 0) {
            LOG.info("Database is empty, initializing sample data...");
            initializeSampleData();
        }
        
        // Log application statistics
        logApplicationStats();
        
        LOG.info("=== Quarkus Trading Application Started Successfully ===");
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("=== Quarkus Trading Application Shutting Down ===");
        
        // Log final statistics
        logApplicationStats();
        
        // Perform cleanup operations
        performCleanup();
        
        LOG.info("=== Quarkus Trading Application Shutdown Complete ===");
    }

    private void initializeSampleData() {
        LOG.info("Creating sample counterparties...");
        
        // Create sample counterparties
        Counterparty bank1 = new Counterparty();
        bank1.name = "Global Investment Bank";
        bank1.code = "GIB001";
        bank1.email = "trading@gib.com";
        bank1.phoneNumber = "+1-555-0101";
        bank1.address = "123 Wall Street, New York, NY 10005";
        bank1.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        bank1.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(bank1);

        Counterparty corp1 = new Counterparty();
        corp1.name = "Tech Solutions Corp";
        corp1.code = "TSC001";
        corp1.email = "finance@techsolutions.com";
        corp1.phoneNumber = "+1-555-0102";
        corp1.address = "456 Silicon Valley Blvd, San Francisco, CA 94105";
        corp1.type = Counterparty.CounterpartyType.CORPORATE;
        corp1.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(corp1);

        Counterparty fund1 = new Counterparty();
        fund1.name = "Alpha Hedge Fund";
        fund1.code = "AHF001";
        fund1.email = "operations@alphafund.com";
        fund1.phoneNumber = "+1-555-0103";
        fund1.address = "789 Financial District, Chicago, IL 60601";
        fund1.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        fund1.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(fund1);

        LOG.info("Creating sample trades...");
        
        // Create sample trades
        Trade trade1 = new Trade();
        trade1.tradeReference = "TRD-2024-001";
        trade1.counterparty = bank1;
        trade1.instrument = "AAPL";
        trade1.tradeType = Trade.TradeType.BUY;
        trade1.quantity = new BigDecimal("1000");
        trade1.price = new BigDecimal("150.25");
        trade1.tradeDate = LocalDate.now().minusDays(5);
        trade1.settlementDate = LocalDate.now().minusDays(3);
        trade1.currency = "USD";
        trade1.status = Trade.TradeStatus.SETTLED;
        trade1.notes = "Initial sample trade for Apple Inc.";
        tradeRepository.persist(trade1);

        Trade trade2 = new Trade();
        trade2.tradeReference = "TRD-2024-002";
        trade2.counterparty = corp1;
        trade2.instrument = "GOOGL";
        trade2.tradeType = Trade.TradeType.SELL;
        trade2.quantity = new BigDecimal("500");
        trade2.price = new BigDecimal("2750.80");
        trade2.tradeDate = LocalDate.now().minusDays(2);
        trade2.settlementDate = LocalDate.now();
        trade2.currency = "USD";
        trade2.status = Trade.TradeStatus.CONFIRMED;
        trade2.notes = "Google stock sale";
        tradeRepository.persist(trade2);

        Trade trade3 = new Trade();
        trade3.tradeReference = "TRD-2024-003";
        trade3.counterparty = fund1;
        trade3.instrument = "MSFT";
        trade3.tradeType = Trade.TradeType.BUY;
        trade3.quantity = new BigDecimal("750");
        trade3.price = new BigDecimal("380.45");
        trade3.tradeDate = LocalDate.now().minusDays(1);
        trade3.settlementDate = LocalDate.now().plusDays(2);
        trade3.currency = "USD";
        trade3.status = Trade.TradeStatus.PENDING;
        trade3.notes = "Microsoft acquisition for portfolio";
        tradeRepository.persist(trade3);

        LOG.info("Sample data initialization completed");
    }

    private void logApplicationStats() {
        long counterpartyCount = counterpartyRepository.count();
        long activeCounterpartyCount = counterpartyRepository.countByStatus(Counterparty.CounterpartyStatus.ACTIVE);
        long tradeCount = tradeRepository.count();
        long pendingTradeCount = tradeRepository.countByStatus(Trade.TradeStatus.PENDING);
        long confirmedTradeCount = tradeRepository.countByStatus(Trade.TradeStatus.CONFIRMED);
        long settledTradeCount = tradeRepository.countByStatus(Trade.TradeStatus.SETTLED);

        LOG.infof("Application Statistics:");
        LOG.infof("  Counterparties: %d total, %d active", counterpartyCount, activeCounterpartyCount);
        LOG.infof("  Trades: %d total (%d pending, %d confirmed, %d settled)", 
                  tradeCount, pendingTradeCount, confirmedTradeCount, settledTradeCount);
    }

    private void performCleanup() {
        LOG.info("Performing application cleanup...");
        
        // Log any pending trades that might need attention
        long pendingCount = tradeRepository.countByStatus(Trade.TradeStatus.PENDING);
        if (pendingCount > 0) {
            LOG.warnf("Application shutting down with %d pending trades", pendingCount);
        }
        
        // Additional cleanup operations could be added here
        // For example: closing connections, flushing caches, etc.
        
        LOG.info("Cleanup operations completed");
    }
}
