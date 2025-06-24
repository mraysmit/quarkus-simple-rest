package dev.mars.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom metrics for the Trading Application.
 * Provides business-specific metrics for monitoring trading operations,
 * counterparty management, and system performance.
 */
@ApplicationScoped
public class TradingMetrics {

    @Inject
    MeterRegistry meterRegistry;

    // Counters for business operations
    private Counter tradesCreated;
    private Counter tradesConfirmed;
    private Counter tradesSettled;
    private Counter tradesFailed;
    private Counter counterpartiesCreated;
    private Counter counterpartiesActivated;
    private Counter counterpartiesDeactivated;

    // Timers for performance monitoring
    private Timer tradeCreationTime;
    private Timer tradeProcessingTime;
    private Timer counterpartyCreationTime;
    private Timer databaseOperationTime;

    // Gauges for current state
    private final AtomicInteger activeTrades = new AtomicInteger(0);
    private final AtomicInteger pendingTrades = new AtomicInteger(0);
    private final AtomicInteger activeCounterparties = new AtomicInteger(0);
    private final AtomicInteger totalTradeValue = new AtomicInteger(0);

    /**
     * Initialize all metrics. Called automatically by CDI.
     */
    public void initializeMetrics() {
        if (tradesCreated != null) {
            return; // Already initialized
        }

        // Trade operation counters
        tradesCreated = Counter.builder("trading.trades.created")
                .description("Total number of trades created")
                .register(meterRegistry);

        tradesConfirmed = Counter.builder("trading.trades.confirmed")
                .description("Total number of trades confirmed")
                .register(meterRegistry);

        tradesSettled = Counter.builder("trading.trades.settled")
                .description("Total number of trades settled")
                .register(meterRegistry);

        tradesFailed = Counter.builder("trading.trades.failed")
                .description("Total number of trades that failed")
                .register(meterRegistry);

        // Counterparty operation counters
        counterpartiesCreated = Counter.builder("trading.counterparties.created")
                .description("Total number of counterparties created")
                .register(meterRegistry);

        counterpartiesActivated = Counter.builder("trading.counterparties.activated")
                .description("Total number of counterparties activated")
                .register(meterRegistry);

        counterpartiesDeactivated = Counter.builder("trading.counterparties.deactivated")
                .description("Total number of counterparties deactivated")
                .register(meterRegistry);

        // Performance timers
        tradeCreationTime = Timer.builder("trading.trades.creation.time")
                .description("Time taken to create a trade")
                .register(meterRegistry);

        tradeProcessingTime = Timer.builder("trading.trades.processing.time")
                .description("Time taken to process a trade")
                .register(meterRegistry);

        counterpartyCreationTime = Timer.builder("trading.counterparties.creation.time")
                .description("Time taken to create a counterparty")
                .register(meterRegistry);

        databaseOperationTime = Timer.builder("trading.database.operation.time")
                .description("Time taken for database operations")
                .register(meterRegistry);

        // State gauges
        Gauge.builder("trading.trades.active", activeTrades, AtomicInteger::get)
                .description("Number of currently active trades")
                .register(meterRegistry);

        Gauge.builder("trading.trades.pending", pendingTrades, AtomicInteger::get)
                .description("Number of currently pending trades")
                .register(meterRegistry);

        Gauge.builder("trading.counterparties.active", activeCounterparties, AtomicInteger::get)
                .description("Number of currently active counterparties")
                .register(meterRegistry);

        Gauge.builder("trading.trades.total.value", totalTradeValue, AtomicInteger::get)
                .description("Total value of all trades")
                .register(meterRegistry);
    }

    // Trade metrics methods
    public void recordTradeCreated(String instrument, String tradeType) {
        initializeMetrics();
        Counter.builder("trading.trades.created")
                .tag("instrument", instrument)
                .tag("type", tradeType)
                .register(meterRegistry)
                .increment();
        activeTrades.incrementAndGet();
        pendingTrades.incrementAndGet();
    }

    public void recordTradeConfirmed(String instrument, String tradeType) {
        initializeMetrics();
        Counter.builder("trading.trades.confirmed")
                .tag("instrument", instrument)
                .tag("type", tradeType)
                .register(meterRegistry)
                .increment();
        pendingTrades.decrementAndGet();
    }

    public void recordTradeSettled(String instrument, String tradeType, double value) {
        initializeMetrics();
        Counter.builder("trading.trades.settled")
                .tag("instrument", instrument)
                .tag("type", tradeType)
                .register(meterRegistry)
                .increment();
        activeTrades.decrementAndGet();
        totalTradeValue.addAndGet((int) value);
    }

    public void recordTradeFailed(String instrument, String tradeType, String errorType) {
        initializeMetrics();
        Counter.builder("trading.trades.failed")
                .tag("instrument", instrument)
                .tag("type", tradeType)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
        activeTrades.decrementAndGet();
        pendingTrades.decrementAndGet();
    }

    // Counterparty metrics methods
    public void recordCounterpartyCreated(String type) {
        initializeMetrics();
        Counter.builder("trading.counterparties.created")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
        activeCounterparties.incrementAndGet();
    }

    public void recordCounterpartyActivated(String type) {
        initializeMetrics();
        Counter.builder("trading.counterparties.activated")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
        activeCounterparties.incrementAndGet();
    }

    public void recordCounterpartyDeactivated(String type) {
        initializeMetrics();
        Counter.builder("trading.counterparties.deactivated")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
        activeCounterparties.decrementAndGet();
    }

    // Performance timing methods
    public Timer.Sample startTradeCreationTimer() {
        initializeMetrics();
        return Timer.start(meterRegistry);
    }

    public void recordTradeCreationTime(Timer.Sample sample, String instrument) {
        initializeMetrics();
        sample.stop(Timer.builder("trading.trades.creation.time")
                .tag("instrument", instrument)
                .register(meterRegistry));
    }

    public Timer.Sample startTradeProcessingTimer() {
        initializeMetrics();
        return Timer.start(meterRegistry);
    }

    public void recordTradeProcessingTime(Timer.Sample sample, String operation) {
        initializeMetrics();
        sample.stop(Timer.builder("trading.trades.processing.time")
                .tag("operation", operation)
                .register(meterRegistry));
    }

    public Timer.Sample startCounterpartyCreationTimer() {
        initializeMetrics();
        return Timer.start(meterRegistry);
    }

    public void recordCounterpartyCreationTime(Timer.Sample sample, String type) {
        initializeMetrics();
        sample.stop(Timer.builder("trading.counterparties.creation.time")
                .tag("type", type)
                .register(meterRegistry));
    }

    public void recordDatabaseOperation(String operation, Duration duration) {
        initializeMetrics();
        Timer.builder("trading.database.operation.time")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(duration);
    }

    // Gauge update methods
    public void updateActiveTradesCount(int count) {
        activeTrades.set(count);
    }

    public void updatePendingTradesCount(int count) {
        pendingTrades.set(count);
    }

    public void updateActiveCounterpartiesCount(int count) {
        activeCounterparties.set(count);
    }

    public void updateTotalTradeValue(int value) {
        totalTradeValue.set(value);
    }

    // Getter methods for testing
    public Counter getTradesCreatedCounter() {
        initializeMetrics();
        return tradesCreated;
    }

    public Counter getTradesConfirmedCounter() {
        initializeMetrics();
        return tradesConfirmed;
    }

    public Timer getTradeCreationTimer() {
        initializeMetrics();
        return tradeCreationTime;
    }

    public AtomicInteger getActiveTradesGauge() {
        return activeTrades;
    }

    public AtomicInteger getPendingTradesGauge() {
        return pendingTrades;
    }
}
