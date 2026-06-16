package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "previous_service")
@Data
@NoArgsConstructor
public class PreviousService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    @JsonIgnore
    private Personnel personnel;

    @Column(length = 100)
    private String serviceType;           // Служба (строкова, контрактна тощо)

    @Column(length = 200)
    private String draftedBy;             // Ким призваний

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;          // Початок періоду

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;            // Кінець періоду

    @Column(length = 50)
    private String rank;                  // Звання

    @Column(length = 100)
    private String militaryUnit;          // Військова частина
}