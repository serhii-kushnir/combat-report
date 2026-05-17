package org.example.repository;

import org.example.entity.PersonnelChild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PersonnelChildRepository extends JpaRepository<PersonnelChild, Long> {
    List<PersonnelChild> findByPersonnelIdOrderByBirthDateAsc(Long personnelId);
    void deleteByPersonnelId(Long personnelId);
}