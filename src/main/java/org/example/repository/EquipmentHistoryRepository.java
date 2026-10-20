package org.example.repository;

import org.example.entity.EquipmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentHistoryRepository extends JpaRepository<EquipmentHistory, Long> {
    List<EquipmentHistory> findByEquipmentIdOrderByChangedAtDesc(Long equipmentId);
}