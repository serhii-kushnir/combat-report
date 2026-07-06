package org.example.repository;

import org.example.entity.Personnel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    // Без пагінації (збережено для експорту та сумісності)
    List<Personnel> findByActiveTrueAndPersonnelStatusOrderByLastNameAsc(String personnelStatus);

    List<Personnel> findByActiveFalseOrderByLastNameAsc();

    List<Personnel> findByLastNameContainingIgnoreCaseAndActiveTrue(String lastName);

    List<Personnel> findByPersonnelStatus(String personnelStatus);

    boolean existsByPersonnelNumber(Integer personnelNumber);

    // З пагінацією
    Page<Personnel> findByActiveTrueAndPersonnelStatus(String personnelStatus, Pageable pageable);

    Page<Personnel> findByLastNameContainingIgnoreCaseAndActiveTrue(String lastName, Pageable pageable);
}