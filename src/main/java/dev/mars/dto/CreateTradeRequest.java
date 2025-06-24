package dev.mars.dto;

import dev.mars.domain.Trade;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateTradeRequest {

    @NotBlank(message = "Trade reference is required")
    @Size(min = 3, max = 50, message = "Trade reference must be between 3 and 50 characters")
    public String tradeReference;

    @NotNull(message = "Counterparty ID is required")
    public Long counterpartyId;

    @NotBlank(message = "Instrument is required")
    @Size(min = 1, max = 100, message = "Instrument must be between 1 and 100 characters")
    public String instrument;

    @NotNull(message = "Trade type is required")
    public Trade.TradeType tradeType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    public BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    public BigDecimal price;

    @NotNull(message = "Trade date is required")
    public LocalDate tradeDate;

    @NotNull(message = "Settlement date is required")
    public LocalDate settlementDate;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    public String currency;

    public Trade.TradeStatus status = Trade.TradeStatus.PENDING;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    public String notes;

    public CreateTradeRequest() {
    }

    public Trade toEntity() {
        Trade trade = new Trade();
        trade.tradeReference = this.tradeReference;
        trade.instrument = this.instrument;
        trade.tradeType = this.tradeType;
        trade.quantity = this.quantity;
        trade.price = this.price;
        trade.tradeDate = this.tradeDate;
        trade.settlementDate = this.settlementDate;
        trade.currency = this.currency;
        trade.status = this.status != null ? this.status : Trade.TradeStatus.PENDING;
        trade.notes = this.notes;
        return trade;
    }
}
