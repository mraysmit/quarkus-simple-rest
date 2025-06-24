package dev.mars.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "counterparties")
public class Counterparty extends PanacheEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    public String name;

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 20, message = "Code must be between 2 and 20 characters")
    @Column(nullable = false, unique = true, length = 20)
    public String code;

    @Email(message = "Email should be valid")
    @Column(length = 100)
    public String email;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Column(length = 20)
    public String phoneNumber;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Column(length = 500)
    public String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CounterpartyType type = CounterpartyType.CORPORATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CounterpartyStatus status = CounterpartyStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(nullable = false)
    public LocalDateTime updatedAt;

    @OneToMany(mappedBy = "counterparty", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Trade> trades;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CounterpartyType {
        INDIVIDUAL,
        CORPORATE,
        INSTITUTIONAL
    }

    public enum CounterpartyStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    // Static finder methods using Panache
    public static Counterparty findByCode(String code) {
        return find("code", code).firstResult();
    }

    public static List<Counterparty> findByType(CounterpartyType type) {
        return find("type", type).list();
    }

    public static List<Counterparty> findByStatus(CounterpartyStatus status) {
        return find("status", status).list();
    }

    public static List<Counterparty> findActiveCounterparties() {
        return find("status", CounterpartyStatus.ACTIVE).list();
    }

    @Override
    public String toString() {
        return "Counterparty{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}
