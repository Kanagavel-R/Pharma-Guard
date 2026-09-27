package com.pharmaguard.service;

import com.pharmaguard.entity.User;
import com.pharmaguard.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String fullName, String email, String password, String role) {
        User user = new User();
        user.setUsername(username != null ? username.trim().toLowerCase() : null);
        user.setFullName(fullName != null ? fullName.trim() : username);
        user.setEmail(email != null ? email.trim().toLowerCase() : null);
        user.setPassword(password);
        user.setRole(role != null ? role.toUpperCase() : "STAFF");
        return registerUser(user);
    }

    public User registerUser(User user) {
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists. Please sign in or use another email.");
        }

        if (user.getUsername() != null && userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("An account with this email or username already exists.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("STAFF");
        }

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    public boolean existsByUsername(String username) {
        if (username == null) return false;
        return userRepository.existsByUsername(username.trim().toLowerCase());
    }

    public Optional<User> findByUsername(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return Optional.empty();
        String search = identifier.trim().toLowerCase();
        return userRepository.findByUsernameOrEmail(search, search);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateProfile(String identifier, String fullName, String email) {
        return updateProfile(identifier, fullName, email, null, null);
    }

    public User updateProfile(String identifier, String fullName, String email, String phoneNumber) {
        return updateProfile(identifier, fullName, email, phoneNumber, null);
    }

    public User updateProfile(String identifier, String fullName, String email, String phoneNumber, String pharmacyName) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("User identifier cannot be empty.");
        }
        String search = identifier.trim().toLowerCase();
        User user = userRepository.findByUsernameOrEmail(search, search)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + identifier));

        if (email != null && !email.trim().isEmpty()) {
            String normalizedEmail = email.trim().toLowerCase();
            if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
                throw new IllegalArgumentException("Email address is already in use by another account.");
            }
            user.setEmail(normalizedEmail);
        }

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }

        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber.trim());
        }

        if (pharmacyName != null && !pharmacyName.trim().isEmpty()) {
            user.setPharmacyName(pharmacyName.trim());
        }

        return userRepository.save(user);
    }

    public User updateProfilePhoto(String identifier, String profilePhoto) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("User identifier cannot be empty.");
        }
        String search = identifier.trim().toLowerCase();
        User user = userRepository.findByUsernameOrEmail(search, search)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + identifier));

        user.setProfilePhoto(profilePhoto);
        return userRepository.save(user);
    }

    public void updateLastLogin(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return;
        try {
            String search = identifier.trim().toLowerCase();
            userRepository.findByUsernameOrEmail(search, search).ifPresent(user -> {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            });
        } catch (Exception ignored) {}
    }

    public void changePassword(String identifier, String currentPassword, String newPassword) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("User identifier cannot be empty.");
        }
        String search = identifier.trim().toLowerCase();
        User user = userRepository.findByUsernameOrEmail(search, search)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + identifier));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean validatePassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}
