package org.example.service;

import org.example.entity.Personnel;
import org.example.repository.PersonnelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PersonnelService {

    private static final Logger log = LoggerFactory.getLogger(PersonnelService.class);
    private final PersonnelRepository repository;

    public PersonnelService(PersonnelRepository repository) {
        this.repository = repository;
    }

    public List<Personnel> getAll() {
        return repository.findByActiveTrueOrderByLastNameAsc();
    }

    public Optional<Personnel> getById(Long id) {
        return repository.findById(id);
    }

    public Personnel create(Personnel p) {
        p.setActive(true);
        if (p.getStatus() == null || p.getStatus().isBlank()) p.setStatus("PRESENT");
        Personnel saved = repository.save(p);
        log.info("Додано бійця: {} (id={})", saved.getFullName(), saved.getId());
        return saved;
    }

    public Personnel update(Long id, Personnel updated) {
        Personnel existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Боєць не знайдений: " + id));
        existing.setLastName(updated.getLastName());
        existing.setFirstName(updated.getFirstName());
        existing.setMiddleName(updated.getMiddleName());
        existing.setRank(updated.getRank());
        existing.setPosition(updated.getPosition());
        existing.setPhone(updated.getPhone());
        existing.setStatus(updated.getStatus());
        existing.setNote(updated.getNote());
        Personnel saved = repository.save(existing);
        log.info("Оновлено бійця: {} (id={})", saved.getFullName(), saved.getId());
        return saved;
    }

    public void deactivate(Long id) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Боєць не знайдений: " + id));
        p.setActive(false);
        repository.save(p);
        log.info("Деактивовано бійця: {} (id={})", p.getFullName(), id);
    }

    public Personnel updateStatus(Long id, String status, String note) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Боєць не знайдений: " + id));
        p.setStatus(status);
        p.setNote(note);
        Personnel saved = repository.save(p);
        log.info("Статус бійця {} змінено на: {}", saved.getFullName(), status);
        return saved;
    }

    public List<Personnel> search(String query) {
        return repository.findByLastNameContainingIgnoreCaseAndActiveTrue(query);
    }
}
