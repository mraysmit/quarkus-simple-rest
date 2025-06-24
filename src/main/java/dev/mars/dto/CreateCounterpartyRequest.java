package dev.mars.dto;

import dev.mars.domain.Counterparty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateCounterpartyRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    public String name;

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 20, message = "Code must be between 2 and 20 characters")
    public String code;

    @Email(message = "Email should be valid")
    public String email;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    public String phoneNumber;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    public String address;

    @NotNull(message = "Type is required")
    public Counterparty.CounterpartyType type;

    public Counterparty.CounterpartyStatus status = Counterparty.CounterpartyStatus.ACTIVE;

    public CreateCounterpartyRequest() {
    }

    public Counterparty toEntity() {
        Counterparty counterparty = new Counterparty();
        counterparty.name = this.name;
        counterparty.code = this.code;
        counterparty.email = this.email;
        counterparty.phoneNumber = this.phoneNumber;
        counterparty.address = this.address;
        counterparty.type = this.type;
        counterparty.status = this.status != null ? this.status : Counterparty.CounterpartyStatus.ACTIVE;
        return counterparty;
    }
}
