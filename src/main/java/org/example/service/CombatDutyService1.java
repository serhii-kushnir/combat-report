//package org.example.service;
//
//import org.example.entity.CombatDuty;
//import org.example.repository.CombatDutyRepository;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class CombatDutyService1 {
//
//    private final CombatDutyRepository dutyRepo;
//
//    public CombatDutyService1(CombatDutyRepository dutyRepo) {
//        this.dutyRepo = dutyRepo;
//    }
//
//    // ----- Без пагінації (для експорту та сумісності) -----
//    @Transactional(readOnly = true)
//    public List<CombatDuty> getAll() {
//        return dutyRepo.findAll();
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<CombatDuty> getById(Long id) {
//        return dutyRepo.findById(id);
//    }
//
//    @Transactional
//    public CombatDuty save(CombatDuty duty) {
//        return dutyRepo.save(duty);
//    }
//
//    @Transactional
//    public void delete(Long id) {
//        dutyRepo.deleteById(id);
//    }
//
//    public boolean hasOverlap(CombatDuty duty, Long excludeId) {
//        return dutyRepo.existsOverlap(duty.getStartTime(), duty.getEndTime(), excludeId);
//    }
//
//    // ----- Нові методи з пагінацією -----
//    public Page<CombatDuty> getPage(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
//        return dutyRepo.findAll(pageable);
//    }
//
//    public Page<CombatDuty> getByYear(int year, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
//        return dutyRepo.findByYear(year, pageable);
//    }
//
//    public Page<CombatDuty> getByYearAndMonth(int year, int month, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
//        return dutyRepo.findByYearAndMonth(year, month, pageable);
//    }
//}