package org.example.repository;

import org.example.entity.FlightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRecordRepository extends JpaRepository<FlightRecord, Long> {

    List<FlightRecord> findAllByOrderByFlightDateAscRecordNumberAsc();

    List<FlightRecord> findByMonthOrderByRecordNumberAsc(String month);

    List<FlightRecord> findByFlightDateBetweenOrderByFlightDateAscRecordNumberAsc(
            LocalDate from, LocalDate to);

    /**
     * Унікальні місяці відсортовані хронологічно.
     * Використовуємо MIN(flightDate) по кожному місяцю —
     * коректно навіть якщо окремі записи мають flightDate = null.
     */
    @Query("SELECT f.month FROM FlightRecord f " +
            "WHERE f.month IS NOT NULL " +
            "GROUP BY f.month " +
            "ORDER BY MIN(f.flightDate) ASC NULLS LAST")
    List<String> findDistinctMonths();

    boolean existsByRecordNumber(Integer recordNumber);

    /**
     * Перевірка чи є записи за вказаним місяцем
     */
    boolean existsByMonth(String month);

    /** Максимальний порядковий номер у журналі */
    @Query("SELECT MAX(f.recordNumber) FROM FlightRecord f")
    Optional<Integer> findMaxRecordNumber();
}