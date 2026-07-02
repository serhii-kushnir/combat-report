package org.example.repository;

import org.example.entity.CombatDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CombatDutyRepository extends JpaRepository<CombatDuty, Long> {

    /**
     * Повертає список унікальних років, у яких є записи в combat_duty.
     */
    @Query("SELECT DISTINCT YEAR(c.startTime) FROM CombatDuty c ORDER BY YEAR(c.startTime) ASC")
    List<Integer> findDistinctYears();

    /**
     * Повертає список унікальних номерів місяців (1–12), у яких є записи в combat_duty.
     */
    @Query("SELECT DISTINCT MONTH(c.startTime) FROM CombatDuty c ORDER BY MONTH(c.startTime) ASC")
    List<Integer> findDistinctMonths();
}