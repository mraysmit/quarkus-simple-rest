package dev.mars.repository;

import dev.mars.domain.Trade;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TradeRepository implements PanacheRepository<Trade> {

    public Optional<Trade> findByTradeReference(String tradeReference) {
        return find("tradeReference", tradeReference).firstResultOptional();
    }

    public List<Trade> findByCounterpartyId(Long counterpartyId) {
        return find("counterparty.id", counterpartyId).list();
    }

    public List<Trade> findByStatus(Trade.TradeStatus status) {
        return find("status", status).list();
    }

    public List<Trade> findByTradeType(Trade.TradeType tradeType) {
        return find("tradeType", tradeType).list();
    }

    public List<Trade> findByInstrument(String instrument) {
        return find("instrument", instrument).list();
    }

    public List<Trade> findByTradeDateRange(LocalDate startDate, LocalDate endDate) {
        return find("tradeDate >= ?1 and tradeDate <= ?2", startDate, endDate).list();
    }

    public List<Trade> findBySettlementDateRange(LocalDate startDate, LocalDate endDate) {
        return find("settlementDate >= ?1 and settlementDate <= ?2", startDate, endDate).list();
    }

    public List<Trade> findPendingTrades() {
        return find("status", Trade.TradeStatus.PENDING).list();
    }

    public List<Trade> findTradesAboveValue(BigDecimal minValue) {
        return find("quantity * price >= ?1", minValue).list();
    }

    public List<Trade> findByCurrency(String currency) {
        return find("currency", currency).list();
    }

    public List<Trade> findAllPaged(int pageIndex, int pageSize) {
        return findAll(Sort.by("tradeDate").descending().and("createdAt", Sort.Direction.Descending))
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    public List<Trade> findByCounterpartyIdPaged(Long counterpartyId, int pageIndex, int pageSize) {
        return find("counterparty.id", counterpartyId)
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    public long countByStatus(Trade.TradeStatus status) {
        return count("status", status);
    }

    public long countByCounterpartyId(Long counterpartyId) {
        return count("counterparty.id", counterpartyId);
    }

    public boolean existsByTradeReference(String tradeReference) {
        return count("tradeReference", tradeReference) > 0;
    }

    public boolean existsByTradeReferenceAndNotId(String tradeReference, Long id) {
        return count("tradeReference = ?1 and id != ?2", tradeReference, id) > 0;
    }

    public BigDecimal getTotalValueByCounterpartyId(Long counterpartyId) {
        return find("SELECT SUM(t.quantity * t.price) FROM Trade t WHERE t.counterparty.id = ?1", counterpartyId)
                .project(BigDecimal.class)
                .firstResult();
    }

    public List<Trade> findTradesWithCounterparty() {
        return find("SELECT t FROM Trade t JOIN FETCH t.counterparty").list();
    }

    public List<Trade> findRecentTrades(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return find("tradeDate >= ?1", cutoffDate)
                .list();
    }
}
