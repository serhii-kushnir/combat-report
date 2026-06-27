package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "personnel_education")
@Data
@NoArgsConstructor
public class PersonnelEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Personnel personnel;

    private String level;             // Вища, Середня спеціальна тощо
    private String institution;       // Заклад освіти
    private String speciality;        // Спеціальність
    private String academicDegree;    // НОВЕ ПОЛЕ: Ступінь (кандидат, доктор тощо)

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String diploma;           // Номер диплому
}