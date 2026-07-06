package org.example.service;

import org.example.entity.Personnel;
import org.example.entity.PersonnelChild;
import org.example.entity.PersonnelEducation;
import org.example.entity.PersonnelWeapon;
import org.example.repository.PersonnelChildRepository;
import org.example.repository.PersonnelEducationRepository;
import org.example.repository.PersonnelRepository;
import org.example.repository.PersonnelWeaponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PersonnelService {

    private static final Logger log = LoggerFactory.getLogger(PersonnelService.class);

    private final PersonnelRepository repository;
    private final PersonnelEducationRepository educationRepository;
    private final PersonnelChildRepository childRepository;
    private final PersonnelWeaponRepository weaponRepository;

    public PersonnelService(PersonnelRepository repository,
                            PersonnelEducationRepository educationRepository,
                            PersonnelChildRepository childRepository,
                            PersonnelWeaponRepository weaponRepository) {
        this.repository = repository;
        this.educationRepository = educationRepository;
        this.childRepository = childRepository;
        this.weaponRepository = weaponRepository;
    }

    // ======================================================================
    //  БЕЗ ПАГІНАЦІЇ (для експорту та інших потреб)
    // ======================================================================

    public List<Personnel> getAll() {
        List<Personnel> list = repository.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        for (Personnel p : list) enrichPersonnel(p);
        return list;
    }

    public List<Personnel> getArchived() {
        List<Personnel> list = repository.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("Не в особовому складі");
        for (Personnel p : list) enrichPersonnel(p);
        return list;
    }

    public List<Personnel> getInactive() {
        return repository.findByActiveFalseOrderByLastNameAsc();
    }

    public List<Personnel> getAllPersonnel() {
        List<Personnel> all = repository.findAll();
        for (Personnel p : all) enrichPersonnel(p);
        return all;
    }

    public List<Personnel> getByStatus(String status) {
        return repository.findByPersonnelStatus(status);
    }

    public Optional<Personnel> getById(Long id) {
        return repository.findById(id);
    }

    public List<Personnel> search(String query) {
        return repository.findByLastNameContainingIgnoreCaseAndActiveTrue(query);
    }

    // ======================================================================
    //  МЕТОДИ З ПАГІНАЦІЄЮ
    // ======================================================================

    public Page<Personnel> getActivePage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<Personnel> pageResult = repository.findByActiveTrueAndPersonnelStatus("В особовому складі", pageable);
        pageResult.forEach(this::enrichPersonnel);
        return pageResult;
    }

    public Page<Personnel> searchPage(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<Personnel> pageResult = repository.findByLastNameContainingIgnoreCaseAndActiveTrue(query, pageable);
        pageResult.forEach(this::enrichPersonnel);
        return pageResult;
    }

    // ======================================================================
    //  СТАТИСТИКА
    // ======================================================================

    public Map<String, Integer> getStatistics() {
        List<Personnel> all = repository.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        int total = all.size();
        int officers = 0, sergeants = 0, soldiers = 0;
        for (Personnel p : all) {
            String rank = p.getRank();
            if (rank == null) continue;
            String lower = rank.toLowerCase();
            if (lower.contains("лейтенант") || lower.contains("капітан") || lower.contains("майор") ||
                    lower.contains("підполковник") || lower.contains("полковник") || lower.contains("генерал")) {
                officers++;
            } else if (lower.contains("сержант") || lower.contains("старшина")) {
                sergeants++;
            } else {
                soldiers++;
            }
        }
        int archived = repository.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("Не в особовому складі").size();

        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("officers", officers);
        stats.put("sergeants", sergeants);
        stats.put("soldiers", soldiers);
        stats.put("archived", archived);
        return stats;
    }

    // ======================================================================
    //  CRUD ОПЕРАЦІЇ
    // ======================================================================

    @Transactional
    public Personnel create(Personnel p) {
        if (p.getPersonnelNumber() != null && repository.existsByPersonnelNumber(p.getPersonnelNumber())) {
            throw new IllegalArgumentException("Порядковий номер " + p.getPersonnelNumber() + " вже зайнятий");
        }
        if (p.getPersonnelStatus() == null || p.getPersonnelStatus().isEmpty()) {
            p.setPersonnelStatus("В особовому складі");
        }
        p.setActive(true);
        Personnel saved = repository.save(p);
        log.info("Додано особу: {} (id={}, номер={})", saved.getFullName(), saved.getId(), saved.getPersonnelNumber());
        return saved;
    }

    @Transactional
    public Personnel update(Long id, Personnel updated) {
        Personnel existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        Integer newNumber = updated.getPersonnelNumber();
        Integer oldNumber = existing.getPersonnelNumber();
        if (newNumber != null && !newNumber.equals(oldNumber) && repository.existsByPersonnelNumber(newNumber)) {
            throw new IllegalArgumentException("Порядковий номер " + newNumber + " вже зайнятий");
        }

        existing.setLastName(updated.getLastName());
        existing.setFirstName(updated.getFirstName());
        existing.setMiddleName(updated.getMiddleName());
        existing.setRank(updated.getRank());
        existing.setPosition(updated.getPosition());
        existing.setFullPosition(updated.getFullPosition());
        existing.setPhone(updated.getPhone());
        existing.setNote(updated.getNote());
        existing.setBirthDate(updated.getBirthDate());
        existing.setTaxId(updated.getTaxId());
        existing.setPassportSeries(updated.getPassportSeries());
        existing.setPassportNumber(updated.getPassportNumber());
        existing.setBloodGroup(updated.getBloodGroup());
        existing.setRegistrationAddress(updated.getRegistrationAddress());
        existing.setLivingAddress(updated.getLivingAddress());
        existing.setMaritalStatus(updated.getMaritalStatus());
        existing.setSpouseName(updated.getSpouseName());
        existing.setDraftDate(updated.getDraftDate());
        existing.setDraftOrganization(updated.getDraftOrganization());
        existing.setServiceType(updated.getServiceType());
        existing.setUbdNumber(updated.getUbdNumber());
        existing.setDriverLicenseSeries(updated.getDriverLicenseSeries());
        existing.setDriverLicenseNumber(updated.getDriverLicenseNumber());
        existing.setDriverLicenseCategory(updated.getDriverLicenseCategory());
        existing.setFamilyAddress(updated.getFamilyAddress());
        existing.setAdmissionForm(updated.getAdmissionForm());
        existing.setEnrollmentInfo(updated.getEnrollmentInfo());
        existing.setServiceFor(updated.getServiceFor());
        existing.setPersonnelNumber(updated.getPersonnelNumber());
        existing.setPersonnelStatus(updated.getPersonnelStatus());
        existing.setVos(updated.getVos());
        existing.setTariffGrade(updated.getTariffGrade());
        existing.setShoeSize(updated.getShoeSize());
        existing.setUniformSize(updated.getUniformSize());
        existing.setHeadwearSize(updated.getHeadwearSize());
        existing.setMilitaryUnit(updated.getMilitaryUnit());
        existing.setDrafObl(updated.getDrafObl());
        existing.setDraftLoc(updated.getDraftLoc());
        existing.setEnrollmentDate(updated.getEnrollmentDate());
        existing.setEnrollmentNakaz(updated.getEnrollmentNakaz());
        existing.setUbdDate(updated.getUbdDate());
        existing.setAdmissionNakaz(updated.getAdmissionNakaz());
        existing.setAdmissionDate(updated.getAdmissionDate());

        Personnel saved = repository.save(existing);
        log.info("Оновлено особу: {} (id={}, номер={})", saved.getFullName(), saved.getId(), saved.getPersonnelNumber());
        return saved;
    }

    @Transactional
    public Personnel patch(Long id, Map<String, Object> updates) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        if (updates.containsKey("personnelNumber")) {
            Object val = updates.get("personnelNumber");
            Integer newNumber = null;
            if (val instanceof Integer) {
                newNumber = (Integer) val;
            } else if (val instanceof String) {
                try {
                    newNumber = Integer.parseInt((String) val);
                } catch (NumberFormatException ignored) {}
            }
            if (newNumber != null && !newNumber.equals(p.getPersonnelNumber()) &&
                    repository.existsByPersonnelNumber(newNumber)) {
                throw new IllegalArgumentException("Порядковий номер " + newNumber + " вже зайнятий");
            }
            p.setPersonnelNumber(newNumber);
            updates.remove("personnelNumber");
        }

        updates.forEach((key, value) -> {
            if (value instanceof String && ((String) value).isEmpty()) value = null;
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
                case "taxId": p.setTaxId(value != null ? String.valueOf(value) : null); break;
                case "passportSeries": p.setPassportSeries(value != null ? String.valueOf(value) : null); break;
                case "passportNumber": p.setPassportNumber(value != null ? String.valueOf(value) : null); break;
                case "bloodGroup": p.setBloodGroup((String) value); break;
                case "driverLicenseSeries": p.setDriverLicenseSeries(value != null ? String.valueOf(value) : null); break;
                case "driverLicenseNumber": p.setDriverLicenseNumber(value != null ? String.valueOf(value) : null); break;
                case "driverLicenseCategory": p.setDriverLicenseCategory(value != null ? String.valueOf(value) : null); break;
                case "registrationAddress": p.setRegistrationAddress((String) value); break;
                case "livingAddress": p.setLivingAddress((String) value); break;
                case "maritalStatus": p.setMaritalStatus((String) value); break;
                case "spouseName": p.setSpouseName((String) value); break;
                case "personnelStatus": p.setPersonnelStatus((String) value); break;
                case "vos": p.setVos((String) value); break;
                case "tariffGrade": p.setTariffGrade((String) value); break;
                case "shoeSize": p.setShoeSize(value != null ? String.valueOf(value) : null); break;
                case "uniformSize": p.setUniformSize(value != null ? String.valueOf(value) : null); break;
                case "headwearSize": p.setHeadwearSize(value != null ? String.valueOf(value) : null); break;
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
                case "militaryUnit": p.setMilitaryUnit((String) value); break;
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

    @Transactional
    public void deactivate(Long id) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));
        p.setActive(false);
        p.setPersonnelStatus("Не в особовому складі");
        repository.save(p);
        log.info("Деактивовано особу: {} (id={})", p.getFullName(), id);
    }

    // ======================================================================
    //  ДОПОМІЖНИЙ МЕТОД ДЛЯ ЗБАГАЧЕННЯ
    // ======================================================================

    private void enrichPersonnel(Personnel p) {
        List<PersonnelEducation> eduList = educationRepository.findByPersonnelIdOrderByStartDateAsc(p.getId());
        if (!eduList.isEmpty()) {
            PersonnelEducation first = eduList.get(0);
            p.setEducation(first.getLevel());
            p.setAcademicDegree(first.getAcademicDegree());
            p.setEducationInstitution(first.getInstitution());
            p.setEducationSpeciality(first.getSpeciality());
            p.setEducationStart(first.getStartDate() != null ? first.getStartDate().toString() : null);
            p.setEducationEnd(first.getEndDate() != null ? first.getEndDate().toString() : null);
            p.setDiploma(first.getDiploma());
        }
        List<PersonnelWeapon> weaponList = weaponRepository.findByPersonnelId(p.getId());
        if (!weaponList.isEmpty()) {
            PersonnelWeapon first = weaponList.get(0);
            p.setWeaponType(first.getWeaponType());
            p.setWeaponSerial(first.getSerialNumber());
            p.setWeaponBayonet(first.getBayonet());
            p.setWeaponMagazines(first.getMagazines());
            p.setWeaponCaliber(first.getCaliber());
            p.setWeaponIssuedDate(first.getIssuedDate());
        }
        List<PersonnelChild> children = childRepository.findByPersonnelIdOrderByBirthDateAsc(p.getId());
        p.setChildrenCount(children.size());
    }
}