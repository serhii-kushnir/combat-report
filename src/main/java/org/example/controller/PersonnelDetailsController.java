package org.example.controller;

import org.example.entity.*;
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
    private final org.example.repository.PersonnelRepository personnelRepo;

    public PersonnelDetailsController(PersonnelEducationRepository eduRepo,
                                      PersonnelChildRepository childRepo,
                                      PersonnelWeaponRepository weaponRepo,
                                      org.example.repository.PersonnelRepository personnelRepo) {
        this.eduRepo = eduRepo;
        this.childRepo = childRepo;
        this.weaponRepo = weaponRepo;
        this.personnelRepo = personnelRepo;
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

    // ===== ОСВІТА: отримати одну =====
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

    // ===== ДІТИ: отримати одну =====
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
            return ResponseEntity.ok(weaponRepo.save(w));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/{id}/weapons/{weaponId}")
    public ResponseEntity<?> deleteWeapon(@PathVariable Long weaponId) {
        weaponRepo.deleteById(weaponId);
        return ResponseEntity.ok().build();
    }

    // ===== ЗБРОЯ: отримати одну =====
    @GetMapping("/api/{id}/weapons/{weaponId}")
    public ResponseEntity<PersonnelWeapon> getWeaponById(@PathVariable Long weaponId) {
        return weaponRepo.findById(weaponId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}