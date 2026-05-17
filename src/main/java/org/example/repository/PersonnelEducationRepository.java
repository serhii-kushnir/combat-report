package org.example.repository;

import org.example.entity.PersonnelEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PersonnelEducationRepository extends JpaRepository<PersonnelEducation, Long> {
    List<PersonnelEducation> findByPersonnelIdOrderByStartDateAsc(Long personnelId);
    void deleteByPersonnelId(Long personnelId);
}