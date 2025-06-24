package dev.mars.service;

import dev.mars.domain.Counterparty;
import dev.mars.domain.Trade;
import dev.mars.dto.CreateTradeRequest;
import dev.mars.dto.TradeDto;
import dev.mars.exception.BusinessException;
import dev.mars.metrics.TradingMetrics;
import dev.mars.repository.CounterpartyRepository;
import dev.mars.repository.TradeRepository;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class TradeService {

    private static final Logger LOG = Logger.getLogger(TradeService.class);

    @Inject
    TradeRepository tradeRepository;

    @Inject
    CounterpartyRepository counterpartyRepository;

    @Inject
    TradingMetrics tradingMetrics;

    public List<TradeDto> getAllTrades() {
        LOG.debug("Fetching all trades");
        return tradeRepository.findTradesWithCounterparty()
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    public List<TradeDto> getAllTradesPaged(int page, int size) {
        LOG.debugf("Fetching trades page %d with size %d", page, size);
        return tradeRepository.findAllPaged(page, size)
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    public Optional<TradeDto> getTradeById(Long id) {
        LOG.debugf("Fetching trade with id: %d", id);
        return tradeRepository.findByIdOptional(id)
                .map(TradeDto::from);
    }

    public Optional<TradeDto> getTradeByReference(String tradeReference) {
        LOG.debugf("Fetching trade with reference: %s", tradeReference);
        return tradeRepository.findByTradeReference(tradeReference)
                .map(TradeDto::from);
    }

    public List<TradeDto> getTradesByCounterpartyId(Long counterpartyId) {
        LOG.debugf("Fetching trades for counterparty id: %d", counterpartyId);
        return tradeRepository.findByCounterpartyId(counterpartyId)
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    public List<TradeDto> getTradesByStatus(Trade.TradeStatus status) {
        LOG.debugf("Fetching trades with status: %s", status);
        return tradeRepository.findByStatus(status)
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    public List<TradeDto> getTradesByDateRange(LocalDate startDate, LocalDate endDate) {
        LOG.debugf("Fetching trades between %s and %s", startDate, endDate);
        return tradeRepository.findByTradeDateRange(startDate, endDate)
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    public List<TradeDto> getPendingTrades() {
        LOG.debug("Fetching pending trades");
        return tradeRepository.findPendingTrades()
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    public List<TradeDto> getRecentTrades(int days) {
        LOG.debugf("Fetching trades from last %d days", days);
        return tradeRepository.findRecentTrades(days)
                .stream()
                .map(TradeDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TradeDto createTrade(@Valid CreateTradeRequest request) {
        LOG.debugf("Creating new trade with reference: %s", request.tradeReference);

        Timer.Sample sample = tradingMetrics.startTradeCreationTimer();

        try {
            // Check if trade reference already exists
            if (tradeRepository.existsByTradeReference(request.tradeReference)) {
                tradingMetrics.recordTradeFailed(request.instrument, request.tradeType.toString(), "DUPLICATE_REFERENCE");
                throw new BusinessException("Trade with reference '" + request.tradeReference + "' already exists");
            }

            // Validate counterparty exists and is active
            Counterparty counterparty = counterpartyRepository.findByIdOptional(request.counterpartyId)
                    .orElseThrow(() -> {
                        tradingMetrics.recordTradeFailed(request.instrument, request.tradeType.toString(), "COUNTERPARTY_NOT_FOUND");
                        return new BusinessException("Counterparty not found with id: " + request.counterpartyId);
                    });

            if (counterparty.status != Counterparty.CounterpartyStatus.ACTIVE) {
                tradingMetrics.recordTradeFailed(request.instrument, request.tradeType.toString(), "COUNTERPARTY_INACTIVE");
                throw new BusinessException("Cannot create trade with inactive counterparty: " + counterparty.code);
            }

            // Validate settlement date is after trade date
            if (request.settlementDate.isBefore(request.tradeDate)) {
                tradingMetrics.recordTradeFailed(request.instrument, request.tradeType.toString(), "INVALID_SETTLEMENT_DATE");
                throw new BusinessException("Settlement date cannot be before trade date");
            }

            Trade trade = request.toEntity();
            trade.counterparty = counterparty;
            tradeRepository.persist(trade);

            // Record successful trade creation metrics
            tradingMetrics.recordTradeCreated(request.instrument, request.tradeType.toString());
            tradingMetrics.recordTradeCreationTime(sample, request.instrument);

            LOG.infof("Created trade with id: %d and reference: %s", trade.id, trade.tradeReference);
            return TradeDto.from(trade);
        } catch (BusinessException e) {
            // Timer is already recorded in specific error cases above
            throw e;
        } catch (Exception e) {
            tradingMetrics.recordTradeFailed(request.instrument, request.tradeType.toString(), "SYSTEM_ERROR");
            throw e;
        }
    }

    @Transactional
    public TradeDto updateTrade(Long id, @Valid CreateTradeRequest request) {
        LOG.debugf("Updating trade with id: %d", id);
        
        Trade trade = tradeRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Trade not found with id: " + id));

        // Check if trade reference already exists for another trade
        if (tradeRepository.existsByTradeReferenceAndNotId(request.tradeReference, id)) {
            throw new BusinessException("Trade with reference '" + request.tradeReference + "' already exists");
        }

        // Validate counterparty exists and is active
        Counterparty counterparty = counterpartyRepository.findByIdOptional(request.counterpartyId)
                .orElseThrow(() -> new BusinessException("Counterparty not found with id: " + request.counterpartyId));

        if (counterparty.status != Counterparty.CounterpartyStatus.ACTIVE) {
            throw new BusinessException("Cannot update trade with inactive counterparty: " + counterparty.code);
        }

        // Validate settlement date is after trade date
        if (request.settlementDate.isBefore(request.tradeDate)) {
            throw new BusinessException("Settlement date cannot be before trade date");
        }

        // Update fields
        trade.tradeReference = request.tradeReference;
        trade.counterparty = counterparty;
        trade.instrument = request.instrument;
        trade.tradeType = request.tradeType;
        trade.quantity = request.quantity;
        trade.price = request.price;
        trade.tradeDate = request.tradeDate;
        trade.settlementDate = request.settlementDate;
        trade.currency = request.currency;
        trade.status = request.status;
        trade.notes = request.notes;

        tradeRepository.persist(trade);
        
        LOG.infof("Updated trade with id: %d", id);
        return TradeDto.from(trade);
    }

    @Transactional
    public TradeDto updateTradeStatus(Long id, Trade.TradeStatus status) {
        LOG.debugf("Updating trade status for id: %d to: %s", id, status);

        Timer.Sample sample = tradingMetrics.startTradeProcessingTimer();

        Trade trade = tradeRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Trade not found with id: " + id));

        Trade.TradeStatus oldStatus = trade.status;
        trade.status = status;
        tradeRepository.persist(trade);

        // Record metrics based on status change
        if (status == Trade.TradeStatus.CONFIRMED && oldStatus == Trade.TradeStatus.PENDING) {
            tradingMetrics.recordTradeConfirmed(trade.instrument, trade.tradeType.toString());
        } else if (status == Trade.TradeStatus.SETTLED && oldStatus == Trade.TradeStatus.CONFIRMED) {
            double tradeValue = trade.quantity.multiply(trade.price).doubleValue();
            tradingMetrics.recordTradeSettled(trade.instrument, trade.tradeType.toString(), tradeValue);
        }

        tradingMetrics.recordTradeProcessingTime(sample, "STATUS_UPDATE");

        LOG.infof("Updated trade status for id: %d to: %s", id, status);
        return TradeDto.from(trade);
    }

    @Transactional
    public void deleteTrade(Long id) {
        LOG.debugf("Deleting trade with id: %d", id);
        
        Trade trade = tradeRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Trade not found with id: " + id));

        // Only allow deletion of pending or cancelled trades
        if (trade.status == Trade.TradeStatus.CONFIRMED || trade.status == Trade.TradeStatus.SETTLED) {
            throw new BusinessException("Cannot delete confirmed or settled trades");
        }

        tradeRepository.delete(trade);
        LOG.infof("Deleted trade with id: %d", id);
    }

    public long getTradeCount() {
        return tradeRepository.count();
    }

    public long getTradeCountByStatus(Trade.TradeStatus status) {
        return tradeRepository.countByStatus(status);
    }

    public BigDecimal getTotalTradeValueByCounterparty(Long counterpartyId) {
        BigDecimal total = tradeRepository.getTotalValueByCounterpartyId(counterpartyId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
