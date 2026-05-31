package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "personnel")
@Data
@NoArgsConstructor
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== ОСНОВНІ =====
    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String firstName;

    private String middleName;
    private String rank;
    private String position;

    @Column(length = 500)
    private String fullPosition;

    private String phone;

    @Column(length = 500)
    private String note;

    @JsonProperty("active")
    private Boolean active = true;

    // ===== ОСОБОВІ ДАНІ =====
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;         // Дата народження

    @Column(length = 20)
    private String taxId;                // ІПН

    @Column(length = 20)
    private String passportSeries;       // Серія паспорта

    @Column(length = 20)
    private String passportNumber;       // Номер паспорта

    @Column(length = 10)
    private String bloodGroup;           // Група крові






    // ===== АДРЕСА =====
    @Column(length = 500)
    private String registrationAddress;  // Адреса реєстрації

    @Column(length = 500)
    private String livingAddress;        // Адреса проживання

    // ===== СІМЕЙНИЙ СТАН =====
    @Column(length = 50)
    private String maritalStatus;        // Сімейний стан

    @Column(length = 500)
    private String spouseName;           // Дружина/чоловік


    // ===== ВІЙСЬКОВІ ДАНІ =====
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate draftDate;         // Дата призову

    @Column(length = 100)
    private String draftOrganization;    // Ким призваний (ТЦК)

    @Column(length = 100)
    private String serviceType;          // Вид служби (контракт/мобілізація)

    @Column(length = 50)
    private String ubdNumber;            // Номер УБД

    // Водійське посвідчення
    @Column(length = 20)
    private String driverLicenseSeries;   // Серія
    @Column(length = 20)
    private String driverLicenseNumber;   // Номер
    @Column(length = 20)
    private String driverLicenseCategory; // Категорія (B, C тощо)

    // ===== ОЗБРОЄННЯ =====

    // ===== АДРЕСА СІМ'Ї =====
    @Column(length = 500)
    private String familyAddress;         // Адреса проживання сім'ї

    // ===== ВІЙСЬКОВІ (розширені) =====
    @Column(length = 255)
    private String admissionForm;         // Форма допуску (Ф-№, наказ, дата)

    @Column(length = 255)
    private String enrollmentInfo;        // Зарахування у в/ч (дата, наказ)

    @Column(length = 100)
    private String serviceFor;            // Військова служба за

    public Personnel(String lastName, String firstName, String middleName,
                     String rank, String position) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.rank = rank;
        this.position = position;
    }

    public String getFullName() {
        return lastName + " " + firstName + " " + (middleName != null ? middleName : "");
    }

    public String getShortName() {
        String i = (firstName != null && !firstName.isEmpty()) ? firstName.substring(0, 1) + "." : "";
        String p = (middleName != null && !middleName.isEmpty()) ? middleName.substring(0, 1) + "." : "";
        return lastName + " " + i + " " + p;
    }
}