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
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String username = "admin_skopa";
            String password = "skopa9099";
            userRepository.findByUsername(username).ifPresentOrElse(
                    user -> {
                        // Якщо користувач існує, оновлюємо пароль (на випадок зміни)
                        if (!passwordEncoder.matches(password, user.getPassword())) {
                            user.setPassword(passwordEncoder.encode(password));
                            userRepository.save(user);
                            System.out.println("✅ Пароль для " + username + " оновлено");
                        }
                    },
                    () -> {
                        userRepository.save(new AppUser(username, passwordEncoder.encode(password), "ADMIN"));
                        System.out.println("✅ Користувача " + username + " створено (пароль: " + password + ")");
                    }
            );
        };
    }
}