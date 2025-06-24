package dev.mars.service;

import dev.mars.domain.Counterparty;
import dev.mars.dto.CounterpartyDto;
import dev.mars.dto.CreateCounterpartyRequest;
import dev.mars.exception.BusinessException;
import dev.mars.metrics.TradingMetrics;
import dev.mars.repository.CounterpartyRepository;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CounterpartyService {

    private static final Logger LOG = Logger.getLogger(CounterpartyService.class);

    @Inject
    CounterpartyRepository counterpartyRepository;

    @Inject
    TradingMetrics tradingMetrics;

    public List<CounterpartyDto> getAllCounterparties() {
        LOG.debug("Fetching all counterparties");
        return counterpartyRepository.listAll()
                .stream()
                .map(CounterpartyDto::from)
                .collect(Collectors.toList());
    }

    public List<CounterpartyDto> getAllCounterpartiesPaged(int page, int size) {
        LOG.debugf("Fetching counterparties page %d with size %d", page, size);
        return counterpartyRepository.findAllPaged(page, size)
                .stream()
                .map(CounterpartyDto::from)
                .collect(Collectors.toList());
    }

    public Optional<CounterpartyDto> getCounterpartyById(Long id) {
        LOG.debugf("Fetching counterparty with id: %d", id);
        return counterpartyRepository.findByIdOptional(id)
                .map(CounterpartyDto::from);
    }

    public Optional<CounterpartyDto> getCounterpartyByCode(String code) {
        LOG.debugf("Fetching counterparty with code: %s", code);
        return counterpartyRepository.findByCode(code)
                .map(CounterpartyDto::from);
    }

    public List<CounterpartyDto> getCounterpartiesByType(Counterparty.CounterpartyType type) {
        LOG.debugf("Fetching counterparties with type: %s", type);
        return counterpartyRepository.findByType(type)
                .stream()
                .map(CounterpartyDto::from)
                .collect(Collectors.toList());
    }

    public List<CounterpartyDto> getActiveCounterparties() {
        LOG.debug("Fetching active counterparties");
        return counterpartyRepository.findActiveCounterparties()
                .stream()
                .map(CounterpartyDto::from)
                .collect(Collectors.toList());
    }

    public List<CounterpartyDto> searchCounterpartiesByName(String name) {
        LOG.debugf("Searching counterparties with name containing: %s", name);
        return counterpartyRepository.findByNameContaining(name)
                .stream()
                .map(CounterpartyDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CounterpartyDto createCounterparty(@Valid CreateCounterpartyRequest request) {
        LOG.debugf("Creating new counterparty with code: %s", request.code);
        
        // Check if code already exists
        if (counterpartyRepository.existsByCode(request.code)) {
            throw new BusinessException("Counterparty with code '" + request.code + "' already exists");
        }

        Counterparty counterparty = request.toEntity();
        counterpartyRepository.persist(counterparty);
        
        LOG.infof("Created counterparty with id: %d and code: %s", counterparty.id, counterparty.code);
        return CounterpartyDto.from(counterparty);
    }

    @Transactional
    public CounterpartyDto updateCounterparty(Long id, @Valid CreateCounterpartyRequest request) {
        LOG.debugf("Updating counterparty with id: %d", id);
        
        Counterparty counterparty = counterpartyRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Counterparty not found with id: " + id));

        // Check if code already exists for another counterparty
        if (counterpartyRepository.existsByCodeAndNotId(request.code, id)) {
            throw new BusinessException("Counterparty with code '" + request.code + "' already exists");
        }

        // Update fields
        counterparty.name = request.name;
        counterparty.code = request.code;
        counterparty.email = request.email;
        counterparty.phoneNumber = request.phoneNumber;
        counterparty.address = request.address;
        counterparty.type = request.type;
        counterparty.status = request.status;

        counterpartyRepository.persist(counterparty);
        
        LOG.infof("Updated counterparty with id: %d", id);
        return CounterpartyDto.from(counterparty);
    }

    @Transactional
    public void deleteCounterparty(Long id) {
        LOG.debugf("Deleting counterparty with id: %d", id);
        
        Counterparty counterparty = counterpartyRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Counterparty not found with id: " + id));

        // Check if counterparty has trades
        if (counterparty.trades != null && !counterparty.trades.isEmpty()) {
            throw new BusinessException("Cannot delete counterparty with existing trades. Please delete trades first.");
        }

        counterpartyRepository.delete(counterparty);
        LOG.infof("Deleted counterparty with id: %d", id);
    }

    @Transactional
    public CounterpartyDto updateCounterpartyStatus(Long id, Counterparty.CounterpartyStatus status) {
        LOG.debugf("Updating counterparty status for id: %d to: %s", id, status);
        
        Counterparty counterparty = counterpartyRepository.findByIdOptional(id)
                .orElseThrow(() -> new BusinessException("Counterparty not found with id: " + id));

        counterparty.status = status;
        counterpartyRepository.persist(counterparty);
        
        LOG.infof("Updated counterparty status for id: %d to: %s", id, status);
        return CounterpartyDto.from(counterparty);
    }

    public long getCounterpartyCount() {
        return counterpartyRepository.count();
    }

    public long getActiveCounterpartyCount() {
        return counterpartyRepository.countByStatus(Counterparty.CounterpartyStatus.ACTIVE);
    }
}
