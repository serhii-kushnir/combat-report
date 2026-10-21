package org.example.repository;

import org.example.entity.Vlk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VlkRepository extends JpaRepository<Vlk, Long> {

    @Query("SELECT v FROM Vlk v JOIN FETCH v.personnel " +
            "WHERE v.startDate <= :yearEnd AND v.endDate >= :yearStart " +
            "ORDER BY v.startDate ASC")
    List<Vlk> findByYear(@Param("yearStart") LocalDate yearStart,
                         @Param("yearEnd") LocalDate yearEnd);

    @Query("SELECT v FROM Vlk v JOIN FETCH v.personnel ORDER BY v.startDate ASC")
    List<Vlk> findAllWithPersonnel();

    @Query("SELECT v FROM Vlk v WHERE v.personnel.id = :personnelId " +
            "ORDER BY v.startDate DESC")
    List<Vlk> findByPersonnelId(@Param("personnelId") Long personnelId);

    @Query("SELECT COUNT(v) > 0 FROM Vlk v " +
            "WHERE v.personnel.id = :personnelId " +
            "AND v.startDate <= :end AND v.endDate >= :start " +
            "AND (:excludeId IS NULL OR v.id != :excludeId)")
    boolean existsOverlap(@Param("personnelId") Long personnelId,
                          @Param("start") LocalDate start,
                          @Param("end") LocalDate end,
                          @Param("excludeId") Long excludeId);

    @Query("SELECT DISTINCT YEAR(v.startDate) FROM Vlk v ORDER BY YEAR(v.startDate) ASC")
    List<Integer> findDistinctYears();
}