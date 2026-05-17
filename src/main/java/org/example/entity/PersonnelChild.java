package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}