package org.example.repository;

import org.example.entity.PersonnelWeapon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PersonnelWeaponRepository extends JpaRepository<PersonnelWeapon, Long> {
    List<PersonnelWeapon> findByPersonnelId(Long personnelId);
    void deleteByPersonnelId(Long personnelId);
}