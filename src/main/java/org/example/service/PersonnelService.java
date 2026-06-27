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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    // ===== ОСНОВНИЙ СПИСОК (активні + статус "В особовому складі") =====
    public List<Personnel> getAll() {
        List<Personnel> list = repository.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("В особовому складі");
        for (Personnel p : list) {
            enrichPersonnel(p);
        }
        return list;
    }

    // ===== АРХІВ (активні + статус "Не в особовому складі") =====
    public List<Personnel> getArchived() {
        List<Personnel> list = repository.findByActiveTrueAndPersonnelStatusOrderByLastNameAsc("Не в особовому складі");
        for (Personnel p : list) {
            enrichPersonnel(p);
        }
        return list;
    }

    // ===== ВСІ НЕАКТИВНІ =====
    public List<Personnel> getInactive() {
        return repository.findByActiveFalseOrderByLastNameAsc();
    }

    // ===== ВСІ ОСОБИ (для експорту / повної таблиці) =====
    public List<Personnel> getAllPersonnel() {
        List<Personnel> all = repository.findAll();
        for (Personnel p : all) {
            enrichPersonnel(p);
        }
        return all;
    }

    // ===== ПОШУК ЗА СТАТУСОМ =====
    public List<Personnel> getByStatus(String status) {
        return repository.findByPersonnelStatus(status);
    }

    public Optional<Personnel> getById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Personnel create(Personnel p) {
        if (p.getPersonnelStatus() == null || p.getPersonnelStatus().isEmpty()) {
            p.setPersonnelStatus("В особовому складі");
        }
        p.setActive(true);
        Personnel saved = repository.save(p);
        log.info("Додано особу: {} (id={})", saved.getFullName(), saved.getId());
        return saved;
    }

    @Transactional
    public Personnel update(Long id, Personnel updated) {
        Personnel e = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        // Копіювання всіх полів
        e.setLastName(updated.getLastName());
        e.setFirstName(updated.getFirstName());
        e.setMiddleName(updated.getMiddleName());
        e.setRank(updated.getRank());
        e.setPosition(updated.getPosition());
        e.setFullPosition(updated.getFullPosition());
        e.setPhone(updated.getPhone());
        e.setNote(updated.getNote());
        e.setBirthDate(updated.getBirthDate());
        e.setTaxId(updated.getTaxId());
        e.setPassportSeries(updated.getPassportSeries());
        e.setPassportNumber(updated.getPassportNumber());
        e.setBloodGroup(updated.getBloodGroup());
        e.setRegistrationAddress(updated.getRegistrationAddress());
        e.setLivingAddress(updated.getLivingAddress());
        e.setMaritalStatus(updated.getMaritalStatus());
        e.setSpouseName(updated.getSpouseName());
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
        e.setPersonnelNumber(updated.getPersonnelNumber());
        e.setPersonnelStatus(updated.getPersonnelStatus());
        e.setVos(updated.getVos());
        e.setTariffGrade(updated.getTariffGrade());
        e.setShoeSize(updated.getShoeSize());
        e.setUniformSize(updated.getUniformSize());
        e.setHeadwearSize(updated.getHeadwearSize());
        e.setMilitaryUnit(updated.getMilitaryUnit());
        e.setDrafObl(updated.getDrafObl());
        e.setDraftLoc(updated.getDraftLoc());
        e.setEnrollmentDate(updated.getEnrollmentDate());
        e.setEnrollmentNakaz(updated.getEnrollmentNakaz());
        e.setUbdDate(updated.getUbdDate());
        e.setAdmissionNakaz(updated.getAdmissionNakaz());
        e.setAdmissionDate(updated.getAdmissionDate());

        Personnel saved = repository.save(e);
        log.info("Оновлено особу: {} (id={})", saved.getFullName(), saved.getId());
        return saved;
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

    @Transactional
    public Personnel patch(Long id, Map<String, Object> updates) {
        Personnel p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Особу не знайдено: " + id));

        updates.forEach((key, value) -> {
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

    public List<Personnel> search(String query) {
        return repository.findByLastNameContainingIgnoreCaseAndActiveTrue(query);
    }

    // ===== ДОПОМІЖНИЙ МЕТОД ДЛЯ ЗБАГАЧЕННЯ =====
    private void enrichPersonnel(Personnel p) {
        // 1. Освіта (перший запис)
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

        // 2. Зброя (перший запис)
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

        // 3. Кількість дітей
        List<PersonnelChild> children = childRepository.findByPersonnelIdOrderByBirthDateAsc(p.getId());
        p.setChildrenCount(children.size());
    }
}