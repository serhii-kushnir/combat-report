package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;          // Назва

    private int quantity;         // Кількість

    private String unit;          // Одиниця виміру

    // НОВІ ПОЛЯ
    private String crew;          // Екіпаж (наприклад, "СКОПА")
    private String location;      // Локація (наприклад, "А0826")
    private String category;      // Категорія (наприклад, "Боєприпаси", "Дрони", "Паливо")

    public Equipment(String name, int quantity, String unit, String crew, String location, String category) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.crew = crew;
        this.location = location;
        this.category = category;
    }
}