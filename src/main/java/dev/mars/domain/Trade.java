package dev.mars.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trades")
public class Trade extends PanacheEntity {

    @NotBlank(message = "Trade reference is required")
    @Size(min = 3, max = 50, message = "Trade reference must be between 3 and 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    public String tradeReference;

    @NotNull(message = "Counterparty is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_id", nullable = false)
    public Counterparty counterparty;

    @NotBlank(message = "Instrument is required")
    @Size(min = 1, max = 100, message = "Instrument must be between 1 and 100 characters")
    @Column(nullable = false, length = 100)
    public String instrument;

    @NotNull(message = "Trade type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TradeType tradeType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal price;

    @NotNull(message = "Trade date is required")
    @Column(nullable = false)
    public LocalDate tradeDate;

    @NotNull(message = "Settlement date is required")
    @Column(nullable = false)
    public LocalDate settlementDate;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Column(nullable = false, length = 3)
    public String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TradeStatus status = TradeStatus.PENDING;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(length = 1000)
    public String notes;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TradeType {
        BUY,
        SELL
    }

    public enum TradeStatus {
        PENDING,
        CONFIRMED,
        SETTLED,
        CANCELLED,
        FAILED
    }

    // Calculated field
    public BigDecimal getTotalValue() {
        if (quantity != null && price != null) {
            return quantity.multiply(price);
        }
        return BigDecimal.ZERO;
    }

    // Static finder methods using Panache
    public static Trade findByTradeReference(String tradeReference) {
        return find("tradeReference", tradeReference).firstResult();
    }

    public static List<Trade> findByCounterparty(Counterparty counterparty) {
        return find("counterparty", counterparty).list();
    }

    public static List<Trade> findByCounterpartyId(Long counterpartyId) {
        return find("counterparty.id", counterpartyId).list();
    }

    public static List<Trade> findByStatus(TradeStatus status) {
        return find("status", status).list();
    }

    public static List<Trade> findByTradeType(TradeType tradeType) {
        return find("tradeType", tradeType).list();
    }

    public static List<Trade> findByInstrument(String instrument) {
        return find("instrument", instrument).list();
    }

    public static List<Trade> findByTradeDateRange(LocalDate startDate, LocalDate endDate) {
        return find("tradeDate >= ?1 and tradeDate <= ?2", startDate, endDate).list();
    }

    public static List<Trade> findPendingTrades() {
        return find("status", TradeStatus.PENDING).list();
    }

    @Override
    public String toString() {
        return "Trade{" +
                "id=" + id +
                ", tradeReference='" + tradeReference + '\'' +
                ", instrument='" + instrument + '\'' +
                ", tradeType=" + tradeType +
                ", quantity=" + quantity +
                ", price=" + price +
                ", status=" + status +
                '}';
    }
}
