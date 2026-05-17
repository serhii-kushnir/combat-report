package org.example.repository;

import org.example.entity.ScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntry, Long> {

    @Query("SELECT s FROM ScheduleEntry s JOIN FETCH s.personnel p " +
           "WHERE s.date >= :from AND s.date <= :to AND p.active = true " +
           "ORDER BY p.lastName, s.date")
    List<ScheduleEntry> findByMonth(@Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    Optional<ScheduleEntry> findByPersonnelIdAndDate(Long personnelId, LocalDate date);
}
