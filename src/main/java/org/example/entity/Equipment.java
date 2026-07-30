package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment")
@Data
@NoArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Кількість на позиції (старе поле quantity)
    private Integer quantity = 0;

    // Кількість на складі
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    // Кількість списано
    @Column(name = "written_off_quantity")
    private Integer writtenOffQuantity = 0;

    private String unit;

    private String crew;

    private String location;

    private String category;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "modified_by")
    private String modifiedBy;

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        lastModified = LocalDateTime.now();
        if (modifiedBy == null) modifiedBy = "system";
    }

    // Конструктор
    public Equipment(String name, Integer quantity, Integer stockQuantity,
                     Integer writtenOffQuantity, String unit, String crew,
                     String location, String category) {
        this.name = name;
        this.quantity = quantity;
        this.stockQuantity = stockQuantity;
        this.writtenOffQuantity = writtenOffQuantity;
        this.unit = unit;
        this.crew = crew;
        this.location = location;
        this.category = category;
    }
}