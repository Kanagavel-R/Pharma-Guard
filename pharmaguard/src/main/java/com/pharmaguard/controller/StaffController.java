package com.pharmaguard.controller;

import com.pharmaguard.entity.User;
import com.pharmaguard.repository.UserRepository;
import com.pharmaguard.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class StaffController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public StaffController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/staff")
    public String viewStaff(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "Admin";
        model.addAttribute("username", username);

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);

        return "staff";
    }

    @PostMapping({"/api/staff/save", "/staff/save"})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveStaffApi(@RequestBody User staffUser, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (staffUser.getEmail() == null || staffUser.getEmail().isBlank()) {
                response.put("success", false);
                response.put("message", "Email is required.");
                return ResponseEntity.badRequest().body(response);
            }
            if (staffUser.getFullName() == null || staffUser.getFullName().isBlank()) {
                response.put("success", false);
                response.put("message", "Full name is required.");
                return ResponseEntity.badRequest().body(response);
            }

            String currentAdmin = (principal != null) ? principal.getName() : "Admin";

            if (staffUser.getId() == null) {
                // New staff registration
                if (userRepository.findByEmail(staffUser.getEmail()).isPresent()) {
                    response.put("success", false);
                    response.put("message", "A user with email '" + staffUser.getEmail() + "' already exists.");
                    return ResponseEntity.badRequest().body(response);
                }

                staffUser.setPassword(passwordEncoder.encode(staffUser.getPassword() != null && !staffUser.getPassword().isBlank() ? staffUser.getPassword() : "PharmaGuard123!"));
                staffUser.setUsername(staffUser.getEmail());
                staffUser.setEnabled(true);
                User saved = userRepository.save(staffUser);

                auditLogService.logAction(currentAdmin, "ADD_STAFF", "User", String.valueOf(saved.getId()), "Registered new staff: " + saved.getFullName() + " (" + saved.getRole() + ")");
                response.put("success", true);
                response.put("message", "✓ New staff member added successfully.");
                response.put("user", saved);
                return ResponseEntity.ok(response);
            } else {
                // Edit staff role / details
                Optional<User> existingOpt = userRepository.findById(staffUser.getId());
                if (existingOpt.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "User not found.");
                    return ResponseEntity.badRequest().body(response);
                }
                User existing = existingOpt.get();
                existing.setFullName(staffUser.getFullName());
                existing.setRole(staffUser.getRole());
                existing.setPhoneNumber(staffUser.getPhoneNumber());
                if (staffUser.getPassword() != null && !staffUser.getPassword().isBlank()) {
                    existing.setPassword(passwordEncoder.encode(staffUser.getPassword()));
                }
                User saved = userRepository.save(existing);

                auditLogService.logAction(currentAdmin, "EDIT_STAFF", "User", String.valueOf(saved.getId()), "Updated staff details for: " + saved.getFullName() + " (" + saved.getRole() + ")");
                response.put("success", true);
                response.put("message", "✓ Staff details updated successfully.");
                response.put("user", saved);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving staff: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/api/staff/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStaffStatus(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String currentAdmin = (principal != null) ? principal.getName() : "Admin";
            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
            user.setEnabled(!user.isEnabled());
            User saved = userRepository.save(user);

            auditLogService.logAction(currentAdmin, "TOGGLE_STAFF_STATUS", "User", String.valueOf(saved.getId()), "Toggled account status for " + saved.getUsername() + " to " + (saved.isEnabled() ? "Active" : "Inactive"));

            response.put("success", true);
            response.put("message", "✓ Account status updated to " + (saved.isEnabled() ? "Active" : "Inactive"));
            response.put("enabled", saved.isEnabled());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
