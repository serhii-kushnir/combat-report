package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personnel")
@Data
@NoArgsConstructor
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String firstName;

    private String middleName;
    private String rank;
    private String position;
    private String phone;

    @Column(length = 50)
    private String status = "PRESENT";

    @Column(length = 500)
    private String note;

    private boolean active = true;

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
