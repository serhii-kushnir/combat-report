package org.example.repository;

import org.example.entity.PersonnelVosTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonnelVosTrainingRepository extends JpaRepository<PersonnelVosTraining, Long> {
    List<PersonnelVosTraining> findByPersonnelIdOrderByStartDateAsc(Long personnelId);
    void deleteByPersonnelId(Long personnelId);
}