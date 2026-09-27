package com.pharmaguard.config;

import com.pharmaguard.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserInitializer implements CommandLineRunner {

    private final UserService userService;

    public UserInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        // Create Admin user if not exists
        if (!userService.existsByUsername("admin") && !userService.existsByEmail("admin@pharmaguard.com")) {
            userService.registerUser("admin", "Dr. Sarah Jenkins (Admin)", "admin@pharmaguard.com", "admin123", "ADMIN");
            System.out.println("Pharma Guard: Default ADMIN user created -> Username: admin | Password: admin123");
        }

        // Create Pharmacist user if not exists
        if (!userService.existsByUsername("pharmacist") && !userService.existsByEmail("pharmacist@pharmaguard.com")) {
            userService.registerUser("pharmacist", "Lead Pharmacist", "pharmacist@pharmaguard.com", "pharma123", "STAFF");
            System.out.println("Pharma Guard: Default PHARMACIST user created -> Username: pharmacist | Password: pharma123");
        }

        // Create Staff user if not exists
        if (!userService.existsByUsername("staff") && !userService.existsByEmail("staff@pharmaguard.com")) {
            userService.registerUser("staff", "Pharmacy Staff Member", "staff@pharmaguard.com", "staff123", "STAFF");
            System.out.println("Pharma Guard: Default STAFF user created -> Username: staff | Password: staff123");
        }
    }
}
