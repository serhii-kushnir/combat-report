package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "personnel_vos_training")
@Data
@NoArgsConstructor
public class PersonnelVosTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    @JsonIgnore
    private Personnel personnel;

    private String name;              // Найменування
    private String speciality;        // Спеціальність
    private String vosNumber;         // ВОС №

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;      // Розпочато

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;        // Закінчено

    private String orderNumber;       // № наказу

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;      // Дата наказу

    private String militaryUnit;      // № В/Ч
}