package org.example.controller;

import org.example.entity.Personnel;
import org.example.entity.PersonnelChild;
import org.example.entity.PersonnelEducation;
import org.example.entity.PersonnelVosTraining;
import org.example.entity.PersonnelWeapon;
import org.example.entity.PreviousService;  // ← ДОДАТИ ЯВНИЙ ІМПОРТ
import org.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/personnel")
public class PersonnelDetailsController {

    private static final Logger log = LoggerFactory.getLogger(PersonnelDetailsController.class);

    private final PersonnelEducationRepository eduRepo;
    private final PersonnelChildRepository childRepo;
    private final PersonnelWeaponRepository weaponRepo;
    private final PersonnelRepository personnelRepo;
    private final PersonnelVosTrainingRepository vosTrainingRepo;
    private final PreviousServiceRepository previousServiceRepo;

    public PersonnelDetailsController(PersonnelEducationRepository eduRepo,
                                      PersonnelChildRepository childRepo,
                                      PersonnelWeaponRepository weaponRepo,
                                      PersonnelRepository personnelRepo,
                                      PersonnelVosTrainingRepository vosTrainingRepo,
                                      PreviousServiceRepository previousServiceRepo) {
        this.eduRepo = eduRepo;
        this.childRepo = childRepo;
        this.weaponRepo = weaponRepo;
        this.personnelRepo = personnelRepo;
        this.vosTrainingRepo = vosTrainingRepo;
        this.previousServiceRepo = previousServiceRepo;
    }

    // ===== ПОПЕРЕДНЯ ВІЙСЬКОВА СЛУЖБА =====

    @GetMapping("/api/{id}/previous-service")
    public List<PreviousService> getPreviousServices(@PathVariable Long id) {
        return previousServiceRepo.findByPersonnelIdOrderByStartDateAsc(id);
    }

    @GetMapping("/api/{id}/previous-service/{serviceId}")
    public ResponseEntity<PreviousService> getPreviousServiceById(@PathVariable Long serviceId) {
        return previousServiceRepo.findById(serviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/{id}/previous-service")
    public ResponseEntity<?> addPreviousService(@PathVariable Long id,
                                                @RequestBody PreviousService service) {
        Personnel p = personnelRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        service.setPersonnel(p);
        service.setId(null);
        log.info("Додано запис попередньої служби для особи id={}", id);
        return ResponseEntity.ok(previousServiceRepo.save(service));
    }

    @PutMapping("/api/{id}/previous-service/{serviceId}")
    public ResponseEntity<?> updatePreviousService(@PathVariable Long serviceId,
                                                   @RequestBody PreviousService service) {
        return previousServiceRepo.findById(serviceId).map(s -> {
            s.setServiceType(service.getServiceType());
            s.setDraftedBy(service.getDraftedBy());
            s.setStartDate(service.getStartDate());
            s.setEndDate(service.getEndDate());
            s.setRank(service.getRank());
            // ПЕРЕКОНАЙТЕСЬ, ЩО НАЗВА ПОЛЯ ВІДПОВІДАЄ СУТНОСТІ
            // Якщо в сутності поле називається "militaryUnit", то ок.
            s.setMilitaryUnit(service.getMilitaryUnit());
            return ResponseEntity.ok(previousServiceRepo.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/{id}/previous-service/{serviceId}")
    public ResponseEntity<?> deletePreviousService(@PathVariable Long serviceId) {
        previousServiceRepo.deleteById(serviceId);
        return ResponseEntity.ok().build();
    }

    // ===== ОСВІТА =====
    @GetMapping("/api/{id}/education")
    public List<PersonnelEducation> getEducationList(@PathVariable Long id) {
        return eduRepo.findByPersonnelIdOrderByStartDateAsc(id);
    }

    @PostMapping("/api/{id}/education")
    public ResponseEntity<?> addEducation(@PathVariable Long id,
                                          @RequestBody PersonnelEducation edu) {
        Personnel p = personnelRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        edu.setPersonnel(p);
        edu.setId(null);
        log.info("Додано освіту для особи id={}: {}", id, edu.getInstitution());
        return ResponseEntity.ok(eduRepo.save(edu));
    }

    @PutMapping("/api/{id}/education/{eduId}")
    public ResponseEntity<?> updateEducation(@PathVariable Long id,
                                             @PathVariable Long eduId,
                                             @RequestBody PersonnelEducation edu) {
        return eduRepo.findById(eduId).map(e -> {
            e.setLevel(edu.getLevel());
            e.setAcademicDegree(edu.getAcademicDegree()); // ДОДАНО
            e.setInstitution(edu.getInstitution());
            e.setSpeciality(edu.getSpeciality());
            e.setStartDate(edu.getStartDate());
            e.setEndDate(edu.getEndDate());
            e.setDiploma(edu.getDiploma());
            return ResponseEntity.ok(eduRepo.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/{id}/education/{eduId}")
    public ResponseEntity<?> deleteEducation(@PathVariable Long eduId) {
        eduRepo.deleteById(eduId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/{id}/education/{eduId}")
    public ResponseEntity<PersonnelEducation> getEducationById(@PathVariable Long eduId) {
        return eduRepo.findById(eduId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== ДІТИ =====
    @GetMapping("/api/{id}/children")
    public List<PersonnelChild> getChildrenList(@PathVariable Long id) {
        return childRepo.findByPersonnelIdOrderByBirthDateAsc(id);
    }

    @PostMapping("/api/{id}/children")
    public ResponseEntity<?> addChild(@PathVariable Long id,
                                      @RequestBody PersonnelChild child) {
        Personnel p = personnelRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        child.setPersonnel(p);
        child.setId(null);
        log.info("Додано дитину для особи id={}: {}", id, child.getFullName());
        return ResponseEntity.ok(childRepo.save(child));
    }

    @PutMapping("/api/{id}/children/{childId}")
    public ResponseEntity<?> updateChild(@PathVariable Long childId,
                                         @RequestBody PersonnelChild child) {
        return childRepo.findById(childId).map(c -> {
            c.setFullName(child.getFullName());
            c.setBirthDate(child.getBirthDate());
            return ResponseEntity.ok(childRepo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/{id}/children/{childId}")
    public ResponseEntity<?> deleteChild(@PathVariable Long childId) {
        childRepo.deleteById(childId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/{id}/children/{childId}")
    public ResponseEntity<PersonnelChild> getChildById(@PathVariable Long childId) {
        return childRepo.findById(childId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== ЗБРОЯ =====
    @GetMapping("/api/{id}/weapons")
    public List<PersonnelWeapon> getWeaponsList(@PathVariable Long id) {
        return weaponRepo.findByPersonnelId(id);
    }

    @PostMapping("/api/{id}/weapons")
    public ResponseEntity<?> addWeapon(@PathVariable Long id,
                                       @RequestBody PersonnelWeapon weapon) {
        Personnel p = personnelRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        weapon.setPersonnel(p);
        weapon.setId(null);
        log.info("Додано зброю для особи id={}: {}", id, weapon.getWeaponType());
        return ResponseEntity.ok(weaponRepo.save(weapon));
    }

    @PutMapping("/api/{id}/weapons/{weaponId}")
    public ResponseEntity<?> updateWeapon(@PathVariable Long weaponId,
                                          @RequestBody PersonnelWeapon weapon) {
        return weaponRepo.findById(weaponId).map(w -> {
            w.setWeaponType(weapon.getWeaponType());
            w.setSerialNumber(weapon.getSerialNumber());
            w.setIssuedDate(weapon.getIssuedDate());
            w.setNote(weapon.getNote());
            w.setBayonet(weapon.getBayonet());
            w.setMagazines(weapon.getMagazines());
            w.setCaliber(weapon.getCaliber());
            return ResponseEntity.ok(weaponRepo.save(w));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/{id}/weapons/{weaponId}")
    public ResponseEntity<?> deleteWeapon(@PathVariable Long weaponId) {
        weaponRepo.deleteById(weaponId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/{id}/weapons/{weaponId}")
    public ResponseEntity<PersonnelWeapon> getWeaponById(@PathVariable Long weaponId) {
        return weaponRepo.findById(weaponId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== ВОС НАВЧАННЯ =====
    @GetMapping("/api/{id}/vos-training")
    public List<PersonnelVosTraining> getVosTraining(@PathVariable Long id) {
        return vosTrainingRepo.findByPersonnelIdOrderByStartDateAsc(id);
    }

    @GetMapping("/api/{id}/vos-training/{trainingId}")
    public ResponseEntity<PersonnelVosTraining> getVosTrainingById(@PathVariable Long trainingId) {
        return vosTrainingRepo.findById(trainingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/{id}/vos-training")
    public ResponseEntity<?> addVosTraining(@PathVariable Long id,
                                            @RequestBody PersonnelVosTraining training) {
        Personnel p = personnelRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        training.setPersonnel(p);
        training.setId(null);
        log.info("Додано ВОС навчання для особи id={}: {}", id, training.getName());
        return ResponseEntity.ok(vosTrainingRepo.save(training));
    }

    @PutMapping("/api/{id}/vos-training/{trainingId}")
    public ResponseEntity<?> updateVosTraining(@PathVariable Long trainingId,
                                               @RequestBody PersonnelVosTraining training) {
        return vosTrainingRepo.findById(trainingId).map(t -> {
            t.setName(training.getName());
            t.setSpeciality(training.getSpeciality());
            t.setVosNumber(training.getVosNumber());
            t.setStartDate(training.getStartDate());
            t.setEndDate(training.getEndDate());
            t.setOrderNumber(training.getOrderNumber());
            t.setOrderDate(training.getOrderDate());
            t.setMilitaryUnit(training.getMilitaryUnit());
            return ResponseEntity.ok(vosTrainingRepo.save(t));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/{id}/vos-training/{trainingId}")
    public ResponseEntity<?> deleteVosTraining(@PathVariable Long trainingId) {
        vosTrainingRepo.deleteById(trainingId);
        return ResponseEntity.ok().build();
    }
}