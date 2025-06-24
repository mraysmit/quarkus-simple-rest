package dev.mars.dto;

import dev.mars.domain.Counterparty;

import java.time.LocalDateTime;

public class CounterpartyDto {
    public Long id;
    public String name;
    public String code;
    public String email;
    public String phoneNumber;
    public String address;
    public Counterparty.CounterpartyType type;
    public Counterparty.CounterpartyStatus status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public int tradeCount;

    public CounterpartyDto() {
    }

    public CounterpartyDto(Counterparty counterparty) {
        this.id = counterparty.id;
        this.name = counterparty.name;
        this.code = counterparty.code;
        this.email = counterparty.email;
        this.phoneNumber = counterparty.phoneNumber;
        this.address = counterparty.address;
        this.type = counterparty.type;
        this.status = counterparty.status;
        this.createdAt = counterparty.createdAt;
        this.updatedAt = counterparty.updatedAt;
        this.tradeCount = counterparty.trades != null ? counterparty.trades.size() : 0;
    }

    public static CounterpartyDto from(Counterparty counterparty) {
        return new CounterpartyDto(counterparty);
    }
}
