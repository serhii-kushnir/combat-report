package org.example.repository;

import org.example.entity.CombatDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // ===== МЕТОД ДЛЯ ПЕРЕВІРКИ ПЕРЕТИНУ (ПУНКТ 4) =====
    /**
     * Перевіряє, чи існує чергування, що перетинається з заданим проміжком.
     * @param startTime початок нового чергування
     * @param endTime кінець нового чергування
     * @param excludeId ID чергування, яке потрібно виключити (для оновлення), або null
     * @return true, якщо є перетин
     */
    @Query("SELECT COUNT(c) > 0 FROM CombatDuty c " +
            "WHERE c.startTime < :endTime AND c.endTime > :startTime " +
            "AND (:excludeId IS NULL OR c.id != :excludeId)")
    boolean existsOverlap(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("excludeId") Long excludeId);

    // ===== МЕТОД ДЛЯ ОПТИМІЗАЦІЇ ЗАВАНТАЖЕННЯ (ПУНКТ 8) =====
    /**
     * Повертає всі чергування, що перетинаються з заданим часовим проміжком.
     */
    @Query("SELECT c FROM CombatDuty c " +
            "WHERE c.startTime <= :endDateTime AND c.endTime >= :startDateTime")
    List<CombatDuty> findOverlapping(@Param("startDateTime") LocalDateTime startDateTime,
                                     @Param("endDateTime") LocalDateTime endDateTime);
}