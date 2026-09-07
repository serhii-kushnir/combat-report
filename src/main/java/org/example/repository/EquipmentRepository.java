package org.example.repository;

import org.example.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Page<Equipment> findByArchivedFalse(Pageable pageable);
    Page<Equipment> findByArchivedTrue(Pageable pageable);

    // ===== ДЛЯ ЗАКРІПЛЕНИХ =====
    List<Equipment> findByPinnedTrueAndArchivedFalseOrderByNameAsc();
    List<Equipment> findByPinnedTrueAndArchivedTrueOrderByNameAsc();

    // ===== ПОШУК =====
    @Query("SELECT e FROM Equipment e WHERE e.archived = false AND LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Equipment> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Equipment e WHERE e.archived = true AND LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Equipment> searchArchived(@Param("search") String search, Pageable pageable);

    // ===== ПЕРЕВІРКА УНІКАЛЬНОСТІ =====
    boolean existsByName(String name);
}