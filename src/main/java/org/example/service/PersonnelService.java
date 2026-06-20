package org.example.service;

import org.example.entity.Personnel;
import org.example.repository.PersonnelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PersonnelService {

    private static final Logger log = LoggerFactory.getLogger(PersonnelService.class);
    private final PersonnelRepository repository;

    public List<Personnel> getByStatus(String status) {
        return repository.findByPersonnelStatus(status);
    }

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

    public Personnel patch(Long id, Map<String, Object> updates) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        updates.forEach((key, value) -> {
            // Якщо значення – порожній рядок, замінюємо на null
            if (value instanceof String && ((String) value).isEmpty()) {
                value = null;
            }

            switch (key) {
                case "lastName": p.setLastName((String) value); break;
                case "firstName": p.setFirstName((String) value); break;
                case "middleName": p.setMiddleName((String) value); break;
                case "rank": p.setRank((String) value); break;
                case "position": p.setPosition((String) value); break;
                case "fullPosition": p.setFullPosition((String) value); break;
                case "phone": p.setPhone((String) value); break;
                case "birthDate":
                    p.setBirthDate(value != null ? LocalDate.parse((String) value) : null);
                    break;
                case "taxId": p.setTaxId((String) value); break;
                case "passportSeries": p.setPassportSeries((String) value); break;
                case "passportNumber": p.setPassportNumber((String) value); break;
                case "bloodGroup": p.setBloodGroup((String) value); break;
                case "driverLicenseSeries": p.setDriverLicenseSeries((String) value); break;
                case "driverLicenseNumber": p.setDriverLicenseNumber((String) value); break;
                case "driverLicenseCategory": p.setDriverLicenseCategory((String) value); break;
                case "registrationAddress": p.setRegistrationAddress((String) value); break;
                case "livingAddress": p.setLivingAddress((String) value); break;
                case "maritalStatus": p.setMaritalStatus((String) value); break;
                case "spouseName": p.setSpouseName((String) value); break;
                case "personnelStatus": p.setPersonnelStatus((String) value); break;
                case "vos": p.setVos((String) value); break;
                case "tariffGrade": p.setTariffGrade((String) value); break;
                case "shoeSize": p.setShoeSize((String) value); break;
                case "uniformSize": p.setUniformSize((String) value); break;
                case "headwearSize": p.setHeadwearSize((String) value); break;
                case "familyAddress": p.setFamilyAddress((String) value); break;
                case "draftDate":
                    p.setDraftDate(value != null ? LocalDate.parse((String) value) : null);
                    break;
                case "draftOrganization": p.setDraftOrganization((String) value); break;
                case "serviceType": p.setServiceType((String) value); break;
                case "ubdNumber": p.setUbdNumber((String) value); break;
                case "admissionForm": p.setAdmissionForm((String) value); break;
                case "enrollmentInfo": p.setEnrollmentInfo((String) value); break;
                case "serviceFor": p.setServiceFor((String) value); break;
                case "note": p.setNote((String) value); break;

                // Нові поля
                case "personnelNumber":
                    if (value instanceof Integer) {
                        p.setPersonnelNumber((Integer) value);
                    } else if (value instanceof String) {
                        try {
                            p.setPersonnelNumber(Integer.parseInt((String) value));
                        } catch (NumberFormatException e) {
                            p.setPersonnelNumber(null);
                        }
                    } else {
                        p.setPersonnelNumber(null);
                    }
                    break;
                case "militaryUnit":
                    p.setMilitaryUnit((String) value);
                    break;
                case "drafObl": p.setDrafObl((String) value); break;
                case "draftLoc": p.setDraftLoc((String) value); break;
                case "enrollmentDate":
                    p.setEnrollmentDate(value != null ? LocalDate.parse((String) value) : null);
                    break;
                case "enrollmentNakaz": p.setEnrollmentNakaz((String) value); break;
                case "ubdDate":
                    p.setUbdDate(value != null ? LocalDate.parse((String) value) : null);
                    break;
                case "admissionNakaz": p.setAdmissionNakaz((String) value); break;
                case "admissionDate":
                    p.setAdmissionDate(value != null ? LocalDate.parse((String) value) : null);
                    break;

                default: log.warn("Невідоме поле для оновлення: {}", key);
            }
        });
        return repository.save(p);
    }

    public List<Personnel> search(String query) {
        return repository.findByLastNameContainingIgnoreCaseAndActiveTrue(query);
    }
}