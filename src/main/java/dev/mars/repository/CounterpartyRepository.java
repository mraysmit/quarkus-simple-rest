package dev.mars.repository;

import dev.mars.domain.Counterparty;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CounterpartyRepository implements PanacheRepository<Counterparty> {

    public Optional<Counterparty> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }

    public List<Counterparty> findByType(Counterparty.CounterpartyType type) {
        return find("type", type).list();
    }

    public List<Counterparty> findByStatus(Counterparty.CounterpartyStatus status) {
        return find("status", status).list();
    }

    public List<Counterparty> findActiveCounterparties() {
        return find("status", Counterparty.CounterpartyStatus.ACTIVE).list();
    }

    public List<Counterparty> findByNameContaining(String name) {
        return find("LOWER(name) LIKE LOWER(?1)", "%" + name + "%").list();
    }

    public List<Counterparty> findAllPaged(int pageIndex, int pageSize) {
        return findAll(Sort.by("name").ascending())
                .page(Page.of(pageIndex, pageSize))
                .list();
    }

    public long countByStatus(Counterparty.CounterpartyStatus status) {
        return count("status", status);
    }

    public boolean existsByCode(String code) {
        return count("code", code) > 0;
    }

    public boolean existsByCodeAndNotId(String code, Long id) {
        return count("code = ?1 and id != ?2", code, id) > 0;
    }

    public List<Counterparty> findCounterpartiesWithTrades() {
        return find("SELECT DISTINCT c FROM Counterparty c JOIN FETCH c.trades").list();
    }

    public List<Counterparty> findCounterpartiesWithoutTrades() {
        return find("SELECT c FROM Counterparty c WHERE c.trades IS EMPTY").list();
    }
}
