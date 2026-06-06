package org.example.security.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String role;   // e.g., "ADMIN", "USER"

    private boolean enabled = true;

    public AppUser(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = true;
    }
}