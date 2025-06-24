package dev.mars.service;

import dev.mars.domain.Counterparty;
import dev.mars.dto.CounterpartyDto;
import dev.mars.dto.CreateCounterpartyRequest;
import dev.mars.exception.BusinessException;
import dev.mars.repository.CounterpartyRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CounterpartyServiceTest {

    @Inject
    CounterpartyService counterpartyService;

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
    void testCreateCounterparty() {
        // Given
        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        request.name = "Test Bank";
        request.code = "TB001";
        request.email = "test@bank.com";
        request.type = Counterparty.CounterpartyType.INSTITUTIONAL;

        // When
        CounterpartyDto result = counterpartyService.createCounterparty(request);

        // Then
        assertNotNull(result.id);
        assertEquals("Test Bank", result.name);
        assertEquals("TB001", result.code);
        assertEquals("test@bank.com", result.email);
        assertEquals(Counterparty.CounterpartyType.INSTITUTIONAL, result.type);
        assertEquals(Counterparty.CounterpartyStatus.ACTIVE, result.status);
    }

    @Test
    @Transactional
    void testCreateCounterpartyWithDuplicateCode() {
        // Given
        CreateCounterpartyRequest request1 = new CreateCounterpartyRequest();
        request1.name = "Bank One";
        request1.code = "DUPLICATE";
        request1.type = Counterparty.CounterpartyType.INSTITUTIONAL;

        CreateCounterpartyRequest request2 = new CreateCounterpartyRequest();
        request2.name = "Bank Two";
        request2.code = "DUPLICATE";
        request2.type = Counterparty.CounterpartyType.CORPORATE;

        // When
        counterpartyService.createCounterparty(request1);

        // Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            counterpartyService.createCounterparty(request2);
        });
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @Transactional
    void testGetCounterpartyById() {
        // Given
        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        request.name = "Test Bank";
        request.code = "TB001";
        request.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        CounterpartyDto created = counterpartyService.createCounterparty(request);

        // When
        Optional<CounterpartyDto> result = counterpartyService.getCounterpartyById(created.id);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Bank", result.get().name);
        assertEquals("TB001", result.get().code);
    }

    @Test
    @Transactional
    void testGetCounterpartyByIdNotFound() {
        // When
        Optional<CounterpartyDto> result = counterpartyService.getCounterpartyById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testGetCounterpartyByCode() {
        // Given
        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        request.name = "Test Bank";
        request.code = "FINDME";
        request.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        counterpartyService.createCounterparty(request);

        // When
        Optional<CounterpartyDto> result = counterpartyService.getCounterpartyByCode("FINDME");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test Bank", result.get().name);
    }

    @Test
    @Transactional
    void testGetAllCounterparties() {
        // Given
        CreateCounterpartyRequest request1 = new CreateCounterpartyRequest();
        request1.name = "Bank One";
        request1.code = "B001";
        request1.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        counterpartyService.createCounterparty(request1);

        CreateCounterpartyRequest request2 = new CreateCounterpartyRequest();
        request2.name = "Bank Two";
        request2.code = "B002";
        request2.type = Counterparty.CounterpartyType.CORPORATE;
        counterpartyService.createCounterparty(request2);

        // When
        List<CounterpartyDto> result = counterpartyService.getAllCounterparties();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    @Transactional
    void testGetCounterpartiesByType() {
        // Given
        CreateCounterpartyRequest institutional = new CreateCounterpartyRequest();
        institutional.name = "Investment Bank";
        institutional.code = "IB001";
        institutional.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        counterpartyService.createCounterparty(institutional);

        CreateCounterpartyRequest corporate = new CreateCounterpartyRequest();
        corporate.name = "Tech Corp";
        corporate.code = "TC001";
        corporate.type = Counterparty.CounterpartyType.CORPORATE;
        counterpartyService.createCounterparty(corporate);

        // When
        List<CounterpartyDto> institutionalResults = counterpartyService.getCounterpartiesByType(Counterparty.CounterpartyType.INSTITUTIONAL);
        List<CounterpartyDto> corporateResults = counterpartyService.getCounterpartiesByType(Counterparty.CounterpartyType.CORPORATE);

        // Then
        assertEquals(1, institutionalResults.size());
        assertEquals("IB001", institutionalResults.get(0).code);
        assertEquals(1, corporateResults.size());
        assertEquals("TC001", corporateResults.get(0).code);
    }

    @Test
    @Transactional
    void testGetActiveCounterparties() {
        // Given
        CreateCounterpartyRequest activeRequest = new CreateCounterpartyRequest();
        activeRequest.name = "Active Bank";
        activeRequest.code = "AB001";
        activeRequest.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        activeRequest.status = Counterparty.CounterpartyStatus.ACTIVE;
        CounterpartyDto active = counterpartyService.createCounterparty(activeRequest);

        CreateCounterpartyRequest inactiveRequest = new CreateCounterpartyRequest();
        inactiveRequest.name = "Inactive Bank";
        inactiveRequest.code = "IB001";
        inactiveRequest.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        inactiveRequest.status = Counterparty.CounterpartyStatus.INACTIVE;
        CounterpartyDto inactive = counterpartyService.createCounterparty(inactiveRequest);

        // Update the inactive one to be inactive
        counterpartyService.updateCounterpartyStatus(inactive.id, Counterparty.CounterpartyStatus.INACTIVE);

        // When
        List<CounterpartyDto> activeCounterparties = counterpartyService.getActiveCounterparties();

        // Then
        assertEquals(1, activeCounterparties.size());
        assertEquals("AB001", activeCounterparties.get(0).code);
    }

    @Test
    @Transactional
    void testUpdateCounterparty() {
        // Given
        CreateCounterpartyRequest createRequest = new CreateCounterpartyRequest();
        createRequest.name = "Original Name";
        createRequest.code = "ORIG001";
        createRequest.type = Counterparty.CounterpartyType.CORPORATE;
        CounterpartyDto created = counterpartyService.createCounterparty(createRequest);

        CreateCounterpartyRequest updateRequest = new CreateCounterpartyRequest();
        updateRequest.name = "Updated Name";
        updateRequest.code = "UPD001";
        updateRequest.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        updateRequest.email = "updated@email.com";

        // When
        CounterpartyDto updated = counterpartyService.updateCounterparty(created.id, updateRequest);

        // Then
        assertEquals(created.id, updated.id);
        assertEquals("Updated Name", updated.name);
        assertEquals("UPD001", updated.code);
        assertEquals("updated@email.com", updated.email);
        assertEquals(Counterparty.CounterpartyType.INSTITUTIONAL, updated.type);
    }

    @Test
    @Transactional
    void testUpdateCounterpartyNotFound() {
        // Given
        CreateCounterpartyRequest updateRequest = new CreateCounterpartyRequest();
        updateRequest.name = "Updated Name";
        updateRequest.code = "UPD001";
        updateRequest.type = Counterparty.CounterpartyType.INSTITUTIONAL;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            counterpartyService.updateCounterparty(999L, updateRequest);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @Transactional
    void testUpdateCounterpartyStatus() {
        // Given
        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        request.name = "Test Bank";
        request.code = "TB001";
        request.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        CounterpartyDto created = counterpartyService.createCounterparty(request);

        // When
        CounterpartyDto updated = counterpartyService.updateCounterpartyStatus(created.id, Counterparty.CounterpartyStatus.SUSPENDED);

        // Then
        assertEquals(Counterparty.CounterpartyStatus.SUSPENDED, updated.status);
    }

    @Test
    @Transactional
    void testDeleteCounterparty() {
        // Given
        CreateCounterpartyRequest request = new CreateCounterpartyRequest();
        request.name = "To Delete";
        request.code = "DEL001";
        request.type = Counterparty.CounterpartyType.CORPORATE;
        CounterpartyDto created = counterpartyService.createCounterparty(request);

        // When
        counterpartyService.deleteCounterparty(created.id);

        // Then
        Optional<CounterpartyDto> result = counterpartyService.getCounterpartyById(created.id);
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testDeleteCounterpartyNotFound() {
        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            counterpartyService.deleteCounterparty(999L);
        });
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @Transactional
    void testSearchCounterpartiesByName() {
        // Given
        CreateCounterpartyRequest request1 = new CreateCounterpartyRequest();
        request1.name = "Global Investment Bank";
        request1.code = "GIB001";
        request1.type = Counterparty.CounterpartyType.INSTITUTIONAL;
        counterpartyService.createCounterparty(request1);

        CreateCounterpartyRequest request2 = new CreateCounterpartyRequest();
        request2.name = "Local Community Bank";
        request2.code = "LCB001";
        request2.type = Counterparty.CounterpartyType.CORPORATE;
        counterpartyService.createCounterparty(request2);

        CreateCounterpartyRequest request3 = new CreateCounterpartyRequest();
        request3.name = "Tech Corporation";
        request3.code = "TC001";
        request3.type = Counterparty.CounterpartyType.CORPORATE;
        counterpartyService.createCounterparty(request3);

        // When
        List<CounterpartyDto> bankResults = counterpartyService.searchCounterpartiesByName("Bank");
        List<CounterpartyDto> globalResults = counterpartyService.searchCounterpartiesByName("Global");

        // Then
        assertEquals(2, bankResults.size());
        assertEquals(1, globalResults.size());
        assertEquals("GIB001", globalResults.get(0).code);
    }
}
