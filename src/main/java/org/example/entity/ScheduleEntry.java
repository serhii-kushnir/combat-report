package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "schedule_entry",
       uniqueConstraints = @UniqueConstraint(columnNames = {"personnel_id", "entry_date"}))
@Data
@NoArgsConstructor
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    private Personnel personnel;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Column(length = 20)
    private String status;

    public ScheduleEntry(Personnel personnel, LocalDate date, String status) {
        this.personnel = personnel;
        this.date = date;
        this.status = status;
    }
}
