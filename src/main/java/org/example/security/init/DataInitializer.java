package org.example.security.init;

import org.example.security.model.AppUser;
import org.example.security.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                userRepository.save(new AppUser("admin", passwordEncoder.encode("admin123"), "ADMIN"));
                System.out.println("Створено admin/admin123");
            }
            if (userRepository.findByUsername("operator").isEmpty()) {
                userRepository.save(new AppUser("operator", passwordEncoder.encode("oper123"), "USER"));
                System.out.println("Створено operator/oper123");
            }
        };
    }
}