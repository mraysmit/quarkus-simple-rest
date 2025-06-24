package dev.mars.repository;

import dev.mars.domain.Counterparty;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CounterpartyRepositoryTest {

    @Inject
    CounterpartyRepository counterpartyRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up before each test - delete trades first to avoid foreign key constraints
        counterpartyRepository.getEntityManager().createQuery("DELETE FROM Trade").executeUpdate();
        counterpartyRepository.deleteAll();
    }

    @Test
    @Transactional
    void testCreateAndFindCounterparty() {
        // Given
        Counterparty counterparty = createTestCounterparty("TEST001", "Test Bank");

        // When
        counterpartyRepository.persist(counterparty);

        // Then
        assertNotNull(counterparty.id);
        Optional<Counterparty> found = counterpartyRepository.findByIdOptional(counterparty.id);
        assertTrue(found.isPresent());
        assertEquals("TEST001", found.get().code);
        assertEquals("Test Bank", found.get().name);
    }

    @Test
    @Transactional
    void testFindByCode() {
        // Given
        Counterparty counterparty = createTestCounterparty("UNIQUE001", "Unique Bank");
        counterpartyRepository.persist(counterparty);

        // When
        Optional<Counterparty> found = counterpartyRepository.findByCode("UNIQUE001");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Unique Bank", found.get().name);
    }

    @Test
    @Transactional
    void testFindByCodeNotFound() {
        // When
        Optional<Counterparty> found = counterpartyRepository.findByCode("NONEXISTENT");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @Transactional
    void testFindByType() {
        // Given
        Counterparty bank = createTestCounterparty("BANK001", "Test Bank");
        bank.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        counterpartyRepository.persist(bank);

        Counterparty corp = createTestCounterparty("CORP001", "Test Corp");
        corp.type = Counterparty.CounterpartyType.CORPORATE;
        counterpartyRepository.persist(corp);

        // When
        List<Counterparty> institutions = counterpartyRepository.findByType(Counterparty.CounterpartyType.INSTITUTIONAL);
        List<Counterparty> corporates = counterpartyRepository.findByType(Counterparty.CounterpartyType.CORPORATE);

        // Then
        assertEquals(1, institutions.size());
        assertEquals("BANK001", institutions.get(0).code);
        assertEquals(1, corporates.size());
        assertEquals("CORP001", corporates.get(0).code);
    }

    @Test
    @Transactional
    void testFindActiveCounterparties() {
        // Given
        Counterparty active = createTestCounterparty("ACTIVE001", "Active Bank");
        active.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(active);

        Counterparty inactive = createTestCounterparty("INACTIVE001", "Inactive Bank");
        inactive.status = Counterparty.CounterpartyStatus.INACTIVE;
        counterpartyRepository.persist(inactive);

        // When
        List<Counterparty> activeCounterparties = counterpartyRepository.findActiveCounterparties();

        // Then
        assertEquals(1, activeCounterparties.size());
        assertEquals("ACTIVE001", activeCounterparties.get(0).code);
    }

    @Test
    @Transactional
    void testFindByNameContaining() {
        // Given
        Counterparty bank1 = createTestCounterparty("BANK001", "Global Investment Bank");
        counterpartyRepository.persist(bank1);

        Counterparty bank2 = createTestCounterparty("BANK002", "Local Community Bank");
        counterpartyRepository.persist(bank2);

        Counterparty corp = createTestCounterparty("CORP001", "Tech Corporation");
        counterpartyRepository.persist(corp);

        // When
        List<Counterparty> bankResults = counterpartyRepository.findByNameContaining("Bank");
        List<Counterparty> globalResults = counterpartyRepository.findByNameContaining("Global");

        // Then
        assertEquals(2, bankResults.size());
        assertEquals(1, globalResults.size());
        assertEquals("BANK001", globalResults.get(0).code);
    }

    @Test
    @Transactional
    void testExistsByCode() {
        // Given
        Counterparty counterparty = createTestCounterparty("EXISTS001", "Existing Bank");
        counterpartyRepository.persist(counterparty);

        // When & Then
        assertTrue(counterpartyRepository.existsByCode("EXISTS001"));
        assertFalse(counterpartyRepository.existsByCode("NOTEXISTS"));
    }

    @Test
    @Transactional
    void testExistsByCodeAndNotId() {
        // Given
        Counterparty counterparty1 = createTestCounterparty("SAME001", "Bank One");
        counterpartyRepository.persist(counterparty1);

        Counterparty counterparty2 = createTestCounterparty("DIFF001", "Bank Two");
        counterpartyRepository.persist(counterparty2);

        // When & Then
        assertFalse(counterpartyRepository.existsByCodeAndNotId("SAME001", counterparty1.id));
        assertTrue(counterpartyRepository.existsByCodeAndNotId("SAME001", counterparty2.id));
    }

    @Test
    @Transactional
    void testCountByStatus() {
        // Given
        Counterparty active1 = createTestCounterparty("ACTIVE001", "Active Bank 1");
        active1.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(active1);

        Counterparty active2 = createTestCounterparty("ACTIVE002", "Active Bank 2");
        active2.status = Counterparty.CounterpartyStatus.ACTIVE;
        counterpartyRepository.persist(active2);

        Counterparty inactive = createTestCounterparty("INACTIVE001", "Inactive Bank");
        inactive.status = Counterparty.CounterpartyStatus.INACTIVE;
        counterpartyRepository.persist(inactive);

        // When
        long activeCount = counterpartyRepository.countByStatus(Counterparty.CounterpartyStatus.ACTIVE);
        long inactiveCount = counterpartyRepository.countByStatus(Counterparty.CounterpartyStatus.INACTIVE);

        // Then
        assertEquals(2, activeCount);
        assertEquals(1, inactiveCount);
    }

    private Counterparty createTestCounterparty(String code, String name) {
        Counterparty counterparty = new Counterparty();
        counterparty.code = code;
        counterparty.name = name;
        counterparty.email = code.toLowerCase() + "@test.com";
        counterparty.type = Counterparty.CounterpartyType.CORPORATE;
        counterparty.status = Counterparty.CounterpartyStatus.ACTIVE;
        return counterparty;
    }
}
