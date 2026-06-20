package org.example.repository;

import org.example.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    // Повертає всіх активних осіб, відсортованих за прізвищем (використовується в інших місцях)
    List<Personnel> findByActiveTrueOrderByLastNameAsc();

    // НОВИЙ МЕТОД – повертає активних осіб із зазначеним статусом, відсортованих за прізвищем
    List<Personnel> findByPersonnelStatusAndActiveTrueOrderByLastNameAsc(String personnelStatus);

    // Пошук за прізвищем (нечіткий, регістронезалежний)
    List<Personnel> findByLastNameContainingIgnoreCaseAndActiveTrue(String lastName);

    List<Personnel> findByPersonnelStatus(String personnelStatus);
}