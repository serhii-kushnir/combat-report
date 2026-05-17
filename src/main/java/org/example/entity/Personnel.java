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

    @Column(length = 100)
    private String driverLicense;        // Водійське посвідчення (категорії)

    // ===== ДОДАТКОВІ ПОЛЯ З ВІДОМОСТІ =====
    @Column(length = 100)
    private String militaryRankShort;    // Військове звання (коротке)
    
    @Column(length = 200)
    private String educationLevel;       // Рівень освіти
    
    @Column(length = 300)
    private String educationInstitution; // Заклад освіти
    
    @Column(length = 200)
    private String educationSpeciality;  // Спеціальність за дипломом (alias: speciality)
    
    @Column(length = 50)
    private String educationForm;        // Форма навчання (денна/заочна)
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate educationStart;    // Дата вступу (alias: educationStartDate)
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate educationEnd;      // Дата закінчення (alias: educationEndDate)
    
    @Column(length = 50)
    private String diplomaNumber;        // Номер диплома
    
    @Column(length = 100)
    private String admissionDate;        // Дата зарахування на службу (alias: enlistmentDate)
    
    @Column(length = 100)
    private String dismissalDate;        // Дата звільнення
    
    @Column(length = 100)
    private String accessForm;           // Форма допуску до держтаємниці (alias: securityClearanceForm)
    
    @Column(length = 100)
    private String accessNumber;         // Номер допуску (alias: securityClearanceOrderNumber)
    
    @Column(length = 200)
    private String militaryUnitPrevious; // Попередня в/ч
    
    @Column(length = 200)
    private String combatExperience;     // Бойовий досвід
    
    @Column(length = 50)
    private String awardStatus;          // Наявність нагород
    
    @Column(length = 300)
    private String awardsList;           // Перелік нагород
    
    @Column(length = 50)
    private String disabilityGroup;      // Група інвалідності
    
    @Column(length = 200)
    private String disabilityCause;      // Причина інвалідності
    
    @Column(length = 50)
    private String mobilizationType;     // Вид мобілізації
    
    @Column(length = 100)
    private String contractStartDate;    // Початок контракту
    
    @Column(length = 100)
    private String contractEndDate;      // Кінець контракту
    
    @Column(length = 100)
    private String salaryCard;           // Зарплатна картка (банк)
    
    @Column(length = 50)
    private String uniformSize;          // Розмір форми
    
    @Column(length = 50)
    private String shoeSize;             // Розмір взуття
    
    @Column(length = 500)
    private String emergencyContact;     // Контактна особа (ПІБ, телефон)
    
    @Column(length = 500)
    private String additionalInfo;       // Додаткова інформація
    
    // ===== ALIAS ПОЛЯ ДЛЯ СУМІСНОСТІ З FRONTEND =====
    @Transient
    public String getSpeciality() {
        return educationSpeciality;
    }
    
    @Transient
    public LocalDate getEducationStartDate() {
        return educationStart;
    }
    
    @Transient
    public LocalDate getEducationEndDate() {
        return educationEnd;
    }
    
    @Transient
    public String getResidenceAddress() {
        return livingAddress;
    }
    
    @Transient
    public String getPositionShort() {
        return rank != null ? rank : position;
    }
    
    @Transient
    public String getRecruitedBy() {
        return draftOrganization;
    }
    
    @Transient
    public String getUbdDocumentNumber() {
        return ubdNumber;
    }
    
    @Transient
    public String getSecurityClearanceForm() {
        return accessForm;
    }
    
    @Transient
    public String getSecurityClearanceOrderNumber() {
        return accessNumber;
    }
    
    @Transient
    public String getEnlistmentDate() {
        return admissionDate;
    }
    
    @Transient
    public String getEnlistmentOrderNumber() {
        return admissionDate;
    }

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