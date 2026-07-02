package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "combat_duty")
@Data
@NoArgsConstructor
public class CombatDuty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "unit_name")
    private String unitName = "СКОПА";

    @Column(length = 255)
    private String commander;

    @Column(length = 255)
    private String pilot;

    @Column(length = 255)
    private String navigator;

    @Column(length = 255)
    private String technician;

    @Column(length = 255)
    private String weapons;

    @Column(length = 255)
    private String dutyOfficer;

    @Column(length = 2000)
    private String reportSummary;

    // === НОВІ ПОЛЯ ДЛЯ ПІДСУМКІВ ===
    private Integer totalSorties = 0;
    private Integer combatSorties = 0;
    private Integer losses = 0;
    private Integer destructions = 0;
    private Integer ntp = 0;
}