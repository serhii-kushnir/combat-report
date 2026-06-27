package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "personnel_child")
@Data
@NoArgsConstructor
public class PersonnelChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Personnel personnel;

    private String fullName;          // ПІБ дитини

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;      // Дата народження

    // ===== НОВИЙ МЕТОД ДЛЯ ОБЧИСЛЕННЯ ВІКУ =====
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}