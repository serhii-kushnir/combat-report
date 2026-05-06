package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Один запис журналу БпАК — один бойовий виліт.
 */
@Entity
@Table(name = "flight_record")
@Data
@NoArgsConstructor
public class FlightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Порядковий номер у журналі (наскрізний) */
    private Integer recordNumber;

    private LocalDate flightDate;
    private String crew;              // Екіпаж (Скопа)
    private String event;             // Подія (Підрив по цілі / Втрата борту)
    private LocalTime takeoffTime;
    private LocalTime lossTime;
    private String coordinates;

    private Integer azimuth;          // Азимут (°)
    private Integer distance;         // Відстань (м)
    private Integer flightAltitude;   // Висота польоту (м) — з параметрів конвертера

    private String lossReason;        // Причина втрати (Відмова батареї, Втрата відео тощо)
    private String targetType;        // БпЛА крило
    private String identification;    // Дружній
    private String weapon;            // Засіб ураження
    private String explosive;         // Вибухівка
    private String detonator;         // Детонатор

    @Column(length = 100)
    private String altitude;          // Висота (може бути "600-4000")

    private String target;            // Ціль (Шахід 8241)
    private Integer targetSpeed;      // Швидкість цілі (км/год)

    @Column(length = 2000)
    private String note;              // Примітка

    @Column(name = "flight_month", length = 30)
    private String month;
}