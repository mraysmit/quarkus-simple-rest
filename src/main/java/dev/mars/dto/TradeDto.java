package dev.mars.dto;

import dev.mars.domain.Trade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TradeDto {
    public Long id;
    public String tradeReference;
    public Long counterpartyId;
    public String counterpartyName;
    public String counterpartyCode;
    public String instrument;
    public Trade.TradeType tradeType;
    public BigDecimal quantity;
    public BigDecimal price;
    public BigDecimal totalValue;
    public LocalDate tradeDate;
    public LocalDate settlementDate;
    public String currency;
    public Trade.TradeStatus status;
    public String notes;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public TradeDto() {
    }

    public TradeDto(Trade trade) {
        this.id = trade.id;
        this.tradeReference = trade.tradeReference;
        this.counterpartyId = trade.counterparty.id;
        this.counterpartyName = trade.counterparty.name;
        this.counterpartyCode = trade.counterparty.code;
        this.instrument = trade.instrument;
        this.tradeType = trade.tradeType;
        this.quantity = trade.quantity;
        this.price = trade.price;
        this.totalValue = trade.getTotalValue();
        this.tradeDate = trade.tradeDate;
        this.settlementDate = trade.settlementDate;
        this.currency = trade.currency;
        this.status = trade.status;
        this.notes = trade.notes;
        this.createdAt = trade.createdAt;
        this.updatedAt = trade.updatedAt;
    }

    public static TradeDto from(Trade trade) {
        return new TradeDto(trade);
    }
}
