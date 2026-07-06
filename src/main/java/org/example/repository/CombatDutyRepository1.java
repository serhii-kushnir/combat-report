//package org.example.repository;
//
//import org.example.entity.CombatDuty;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Repository
//public interface CombatDutyRepository1 extends JpaRepository<CombatDuty, Long> {
//
//    @Query("SELECT DISTINCT YEAR(c.startTime) FROM CombatDuty c ORDER BY YEAR(c.startTime) ASC")
//    List<Integer> findDistinctYears();
//
//    @Query("SELECT DISTINCT MONTH(c.startTime) FROM CombatDuty c ORDER BY MONTH(c.startTime) ASC")
//    List<Integer> findDistinctMonths();
//
//    // Перевірка перетину (пункт 4)
//    @Query("SELECT COUNT(c) > 0 FROM CombatDuty c " +
//           "WHERE c.startTime < :endTime AND c.endTime > :startTime " +
//           "AND (:excludeId IS NULL OR c.id != :excludeId)")
//    boolean existsOverlap(@Param("startTime") LocalDateTime startTime,
//                          @Param("endTime") LocalDateTime endTime,
//                          @Param("excludeId") Long excludeId);
//
//    // Оптимізація завантаження (пункт 8)
//    @Query("SELECT c FROM CombatDuty c " +
//           "WHERE c.startTime <= :endDateTime AND c.endTime >= :startDateTime")
//    List<CombatDuty> findOverlapping(@Param("startDateTime") LocalDateTime startDateTime,
//                                     @Param("endDateTime") LocalDateTime endDateTime);
//
//    // ----- Нові методи з пагінацією -----
//    @Query("SELECT c FROM CombatDuty c WHERE YEAR(c.startTime) = :year AND MONTH(c.startTime) = :month")
//    Page<CombatDuty> findByYearAndMonth(@Param("year") int year,
//                                        @Param("month") int month,
//                                        Pageable pageable);
//
//    @Query("SELECT c FROM CombatDuty c WHERE YEAR(c.startTime) = :year")
//    Page<CombatDuty> findByYear(@Param("year") int year, Pageable pageable);
//}