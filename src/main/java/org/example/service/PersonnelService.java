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
        Personnel saved = repository.save(p);
        log.info("Додано особу: {} (id={})", saved.getFullName(), saved.getId());
        return saved;
    }

    public Personnel update(Long id, Personnel updated) {
        Personnel e = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        // Основні
        e.setLastName(updated.getLastName());
        e.setFirstName(updated.getFirstName());
        e.setMiddleName(updated.getMiddleName());
        e.setRank(updated.getRank());
        e.setPosition(updated.getPosition());
        e.setFullPosition(updated.getFullPosition());
        e.setPhone(updated.getPhone());
        e.setNote(updated.getNote());

        // Особові дані
        e.setBirthDate(updated.getBirthDate());
        e.setTaxId(updated.getTaxId());
        e.setPassportSeries(updated.getPassportSeries());
        e.setPassportNumber(updated.getPassportNumber());
        e.setBloodGroup(updated.getBloodGroup());

        // Адреса
        e.setRegistrationAddress(updated.getRegistrationAddress());
        e.setLivingAddress(updated.getLivingAddress());

        // Сімейний стан
        e.setMaritalStatus(updated.getMaritalStatus());
        e.setSpouseName(updated.getSpouseName());

        // Військові дані
        e.setDraftDate(updated.getDraftDate());
        e.setDraftOrganization(updated.getDraftOrganization());
        e.setServiceType(updated.getServiceType());
        e.setUbdNumber(updated.getUbdNumber());
        e.setDriverLicenseSeries(updated.getDriverLicenseSeries());
        e.setDriverLicenseNumber(updated.getDriverLicenseNumber());
        e.setDriverLicenseCategory(updated.getDriverLicenseCategory());
        e.setFamilyAddress(updated.getFamilyAddress());
        e.setAdmissionForm(updated.getAdmissionForm());
        e.setEnrollmentInfo(updated.getEnrollmentInfo());
        e.setServiceFor(updated.getServiceFor());

        Personnel saved = repository.save(e);
        log.info("Оновлено особу: {} (id={})", saved.getFullName(), saved.getId());
        return saved;
    }

    public void deactivate(Long id) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));
        p.setActive(false);
        repository.save(p);
        log.info("Деактивовано особу: {} (id={})", p.getFullName(), id);
    }

    public List<Personnel> search(String query) {
        return repository.findByLastNameContainingIgnoreCaseAndActiveTrue(query);
    }
}