package org.example.repository;

import org.example.entity.CombatDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CombatDutyRepository extends JpaRepository<CombatDuty, Long> {
    // Метод findByIdWithEvents ВИДАЛЕНО
}