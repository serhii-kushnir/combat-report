package org.example.repository;

import org.example.entity.PpdScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PpdScheduleRepository extends JpaRepository<PpdScheduleEntry, Long> {

    @Query("SELECT DISTINCT YEAR(s.date) FROM PpdScheduleEntry s ORDER BY YEAR(s.date) ASC")
    List<Integer> findDistinctYears();

    @Query("SELECT DISTINCT MONTH(s.date) FROM PpdScheduleEntry s ORDER BY MONTH(s.date) ASC")
    List<Integer> findDistinctMonthNumbers();

    @Query("SELECT s FROM PpdScheduleEntry s JOIN FETCH s.personnel p " +
           "WHERE s.date >= :from AND s.date <= :to AND p.active = true " +
           "ORDER BY p.lastName, s.date")
    List<PpdScheduleEntry> findByMonth(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    Optional<PpdScheduleEntry> findByPersonnelIdAndDate(Long personnelId, LocalDate date);
}