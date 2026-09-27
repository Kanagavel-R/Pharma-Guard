package com.pharmaguard.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @NotBlank(message = "Full name is required")
    @Column(nullable = false)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "STAFF";

    private Boolean enabled = true;

    private String phoneNumber;

    @Lob
    @Column(length = 10000000)
    private String profilePhoto;

    private String pharmacyName = "Pharma Guard Central Pharmacy";

    private String employeeId;

    private LocalDateTime lastLogin;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.username == null || this.username.trim().isEmpty()) {
            this.username = this.email;
        }
    }

    public String getFirstName() {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    public String getLastName() {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    public String getInitials() {
        if (fullName == null || fullName.isBlank()) return "A";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    public String getEmployeeId() {
        if (employeeId != null && !employeeId.isBlank()) return employeeId;
        String prefix = role != null ? (role.length() >= 3 ? role.substring(0, 3).toUpperCase() : role.toUpperCase()) : "USR";
        long num = id != null ? id : 1L;
        return String.format("PG-%s-%03d", prefix, num);
    }

    public String getDisplayRole() {
        if ("ADMIN".equalsIgnoreCase(role)) return "Administrator";
        if ("PHARMACIST".equalsIgnoreCase(role)) return "Pharmacist";
        if ("STAFF".equalsIgnoreCase(role)) return "Pharmacy Staff";
        return role != null ? role : "Staff";
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public String getAccountStatusText() {
        return isEnabled() ? "Active" : "Inactive";
    }
}
