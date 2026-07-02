package org.example.service;

import org.example.entity.CombatDuty;
import org.example.repository.CombatDutyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CombatDutyService {

    private final CombatDutyRepository dutyRepo;

    public CombatDutyService(CombatDutyRepository dutyRepo) {
        this.dutyRepo = dutyRepo;
    }

    @Transactional(readOnly = true)
    public List<CombatDuty> getAll() {
        return dutyRepo.findAll();
    }

    public boolean hasOverlap(CombatDuty duty, Long excludeId) {
        return dutyRepo.existsOverlap(duty.getStartTime(), duty.getEndTime(), excludeId);
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
}