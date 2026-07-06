package org.example.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "personnel")
@Data
@NoArgsConstructor
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer personnelNumber;

    @Column(nullable = false)
    private String lastName;

    @Column(length = 10)
    private String shoeSize;

    @Column(length = 10)
    private String uniformSize;

    @Column(length = 10)
    private String headwearSize;

    @Column(length = 50)
    private String vos;

    @Column(length = 30)
    private String tariffGrade;

    @Column(length = 100)
    private String personnelStatus;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Column(length = 50)
    private String taxId;

    @Column(length = 50)
    private String passportSeries;

    @Column(length = 50)
    private String passportNumber;

    @Column(length = 50)
    private String bloodGroup;

    @Column(length = 50)
    private String education;

    @Column(length = 100)
    private String militaryUnit;

    @Column(length = 100)
    private String drafObl;

    @Column(length = 100)
    private String draftLoc;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate enrollmentDate;

    @Column(length = 100)
    private String enrollmentNakaz;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ubdDate;

    @Column(length = 50)
    private String admissionNakaz;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate admissionDate;

    @Column(length = 500)
    private String registrationAddress;

    @Column(length = 500)
    private String livingAddress;

    @Column(length = 50)
    private String maritalStatus;

    @Column(length = 500)
    private String spouseName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate draftDate;

    @Column(length = 100)
    private String draftOrganization;

    @Column(length = 100)
    private String serviceType;

    @Column(length = 100)
    private String ubdNumber;

    @Column(length = 50)
    private String driverLicenseSeries;

    @Column(length = 50)
    private String driverLicenseNumber;

    @Column(length = 100)
    private String driverLicenseCategory;

    @Column(length = 500)
    private String familyAddress;

    @Column(length = 255)
    private String admissionForm;

    @Column(length = 255)
    private String enrollmentInfo;

    @Column(length = 100)
    private String serviceFor;

    // ===== ЗВ'ЯЗКИ З @BatchSize ТА @JsonIgnore =====
    @JsonIgnore
    @OneToMany(mappedBy = "personnel", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    private List<PersonnelEducation> educationList = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "personnel", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    private List<PersonnelChild> childrenList = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "personnel", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 20)
    private List<PersonnelWeapon> weaponList = new ArrayList<>();

    // ===== TRANSIENT ПОЛЯ =====
    @Transient
    private String educationInstitution;
    @Transient
    private String educationSpeciality;
    @Transient
    private String educationStart;
    @Transient
    private String educationEnd;
    @Transient
    private String diploma;
    @Transient
    private String academicDegree;

    @Transient
    private String weaponType;
    @Transient
    private String weaponSerial;
    @Transient
    private String weaponBayonet;
    @Transient
    private String weaponMagazines;
    @Transient
    private String weaponCaliber;
    @Transient
    private String weaponIssuedDate;

    @Transient
    private Integer childrenCount;

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

    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}