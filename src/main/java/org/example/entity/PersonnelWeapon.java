package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personnel_weapon")
@Data
@NoArgsConstructor
public class PersonnelWeapon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Personnel personnel;

    private String weaponType;        // Тип (АК-74, Пістолет тощо)
    private String serialNumber;      // Серійний номер
    private String issuedDate;        // Дата видачі
    private String note;              // Примітка
    private String bayonet;           // Штик-багнет
    private String magazines;         // Магазини
    private String caliber;           // Калібр (наприклад, 5.45, 7.62, 9 мм)
}