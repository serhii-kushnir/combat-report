package org.example.repository;

import org.example.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    @Query("SELECT v FROM Vacation v JOIN FETCH v.personnel " +
           "WHERE v.startDate <= :yearEnd AND v.endDate >= :yearStart " +
           "ORDER BY v.personnel.lastName, v.startDate")
    List<Vacation> findByYear(@Param("yearStart") LocalDate yearStart,
                              @Param("yearEnd") LocalDate yearEnd);

    @Query("SELECT v FROM Vacation v WHERE v.personnel.id = :personnelId " +
           "ORDER BY v.startDate DESC")
    List<Vacation> findByPersonnelId(@Param("personnelId") Long personnelId);

    @Query("SELECT COUNT(v) > 0 FROM Vacation v " +
           "WHERE v.personnel.id = :personnelId " +
           "AND v.startDate <= :end AND v.endDate >= :start " +
           "AND (:excludeId IS NULL OR v.id != :excludeId)")
    boolean existsOverlap(@Param("personnelId") Long personnelId,
                          @Param("start") LocalDate start,
                          @Param("end") LocalDate end,
                          @Param("excludeId") Long excludeId);

    @Query("SELECT DISTINCT YEAR(v.startDate) FROM Vacation v ORDER BY YEAR(v.startDate) ASC")
    List<Integer> findDistinctYears();
}