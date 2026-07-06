package org.example.repository;

import org.example.entity.FlightRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRecordRepository extends JpaRepository<FlightRecord, Long> {

    // ----- Існуючі методи -----
    List<FlightRecord> findAllByOrderByFlightDateAscRecordNumberAsc();

    List<FlightRecord> findByMonthOrderByRecordNumberAsc(String month);

    List<FlightRecord> findByFlightDateBetweenOrderByFlightDateAscRecordNumberAsc(LocalDate from, LocalDate to);

    @Query("SELECT f.month FROM FlightRecord f WHERE f.month IS NOT NULL GROUP BY f.month ORDER BY MIN(f.flightDate) ASC NULLS LAST")
    List<String> findDistinctMonths();

    boolean existsByRecordNumber(Integer recordNumber);

    boolean existsByMonth(String month);

    @Query("SELECT DISTINCT YEAR(f.flightDate) FROM FlightRecord f WHERE f.flightDate IS NOT NULL ORDER BY 1 ASC")
    List<Integer> findDistinctYears();

    @Query("SELECT MAX(f.recordNumber) FROM FlightRecord f")
    Optional<Integer> findMaxRecordNumber();

    // ----- Нові методи з пагінацією -----
    Page<FlightRecord> findByMonth(String month, Pageable pageable);

    Page<FlightRecord> findByFlightDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    // зручний метод для року (використовує BETWEEN)
    default Page<FlightRecord> findByYear(int year, Pageable pageable) {
        return findByFlightDateBetween(LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31), pageable);
    }
}