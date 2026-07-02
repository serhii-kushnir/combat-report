package org.example.repository;

import org.example.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    boolean existsByPersonnelNumber(Integer personnelNumber);

    // Основний список (активні + статус "В особовому складі")
    List<Personnel> findByActiveTrueAndPersonnelStatusOrderByLastNameAsc(String personnelStatus);

    // Всі неактивні (для повного приховування)
    List<Personnel> findByActiveFalseOrderByLastNameAsc();

    // Пошук за прізвищем серед активних
    List<Personnel> findByLastNameContainingIgnoreCaseAndActiveTrue(String lastName);

    // За статусом (будь-які)
    List<Personnel> findByPersonnelStatus(String personnelStatus);
}