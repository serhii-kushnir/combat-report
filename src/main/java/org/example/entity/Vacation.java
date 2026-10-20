package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "vacation")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vacation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id", nullable = false)
    @JsonIgnore
    private Personnel personnel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "vacation_type", length = 100)
    private String vacationType;

    @Column(length = 500)
    private String note;

    // ===== Зручні поля для JSON =====
    @JsonProperty("personnelId")
    public Long getPersonnelId() {
        return personnel != null ? personnel.getId() : null;
    }

    @JsonProperty("personnelName")
    public String getPersonnelName() {
        return personnel != null ? personnel.getShortName() : null;
    }

    @JsonProperty("rank")
    public String getPersonnelRank() {
        return personnel != null ? personnel.getRank() : null;
    }

    @JsonProperty("personnelNumber")
    public Integer getPersonnelNumber() {
        return personnel != null ? personnel.getPersonnelNumber() : null;
    }
}