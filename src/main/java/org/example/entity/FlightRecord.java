package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "flight_record")
@Data
@NoArgsConstructor
public class FlightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer recordNumber;
    private LocalDate flightDate;
    private String crew;
    private String event;
    private LocalTime takeoffTime;
    private LocalTime lossTime;
    private String coordinates;

    private Integer azimuth;          // Азимут (°)
    private Integer courseDirection;  // НОВЕ ПОЛЕ: Курс (°)
    private Integer distance;         // Відстань (м)
    private Integer flightAltitude;

    private String lossReason;
    private String targetType;
    private String identification;
    private String weapon;
    private String explosive;
    private String detonator;

    @Column(length = 100)
    private String altitude;

    private Integer targetAltitude;
    private String target;
    private Integer targetSpeed;

    @Column(length = 2000)
    private String note;

    @Column(name = "flight_month", length = 30)
    private String month;
}