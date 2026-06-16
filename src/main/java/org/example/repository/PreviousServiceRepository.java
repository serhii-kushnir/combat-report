package org.example.repository;

import org.example.entity.PreviousService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreviousServiceRepository extends JpaRepository<PreviousService, Long> {
    List<PreviousService> findByPersonnelIdOrderByStartDateAsc(Long personnelId);
    void deleteByPersonnelId(Long personnelId);
}