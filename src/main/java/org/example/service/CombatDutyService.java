package org.example.service;

import org.example.entity.CombatDuty;
import org.example.repository.CombatDutyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CombatDutyService {

    private final CombatDutyRepository dutyRepo;

    public CombatDutyService(CombatDutyRepository dutyRepo) {
        this.dutyRepo = dutyRepo;
    }

    // ===== БЕЗ ПАГІНАЦІЇ (для експорту та фільтрів) =====
    @Transactional(readOnly = true)
    public List<CombatDuty> getAll() {
        return dutyRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CombatDuty> getById(Long id) {
        return dutyRepo.findById(id);
    }

    @Transactional
    public CombatDuty save(CombatDuty duty) {
        return dutyRepo.save(duty);
    }

    @Transactional
    public void delete(Long id) {
        dutyRepo.deleteById(id);
    }

    public boolean hasOverlap(CombatDuty duty, Long excludeId) {
        return dutyRepo.existsOverlap(duty.getStartTime(), duty.getEndTime(), excludeId);
    }

    // ===== МЕТОДИ З ПАГІНАЦІЄЮ =====
    public Page<CombatDuty> getPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return dutyRepo.findAll(pageable);
    }

    public Page<CombatDuty> getByYear(int year, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return dutyRepo.findByYear(year, pageable);
    }

    public Page<CombatDuty> getByYearAndMonth(int year, int month, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return dutyRepo.findByYearAndMonth(year, month, pageable);
    }

    // ===== ЗАГАЛЬНА СТАТИСТИКА (НОВИЙ МЕТОД) =====
    @Transactional(readOnly = true)
    public Map<String, Long> getGeneralStats() {
        List<CombatDuty> all = dutyRepo.findAll();
        long total = all.size();
        long totalSorties = all.stream().mapToLong(d -> d.getTotalSorties() != null ? d.getTotalSorties() : 0).sum();
        long combatSorties = all.stream().mapToLong(d -> d.getCombatSorties() != null ? d.getCombatSorties() : 0).sum();
        long losses = all.stream().mapToLong(d -> d.getLosses() != null ? d.getLosses() : 0).sum();
        long destructions = all.stream().mapToLong(d -> d.getDestructions() != null ? d.getDestructions() : 0).sum();
        long ntp = all.stream().mapToLong(d -> d.getNtp() != null ? d.getNtp() : 0).sum();

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("totalSorties", totalSorties);
        stats.put("combatSorties", combatSorties);
        stats.put("losses", losses);
        stats.put("destructions", destructions);
        stats.put("ntp", ntp);
        return stats;
    }
}