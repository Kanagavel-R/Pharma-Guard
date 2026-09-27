package com.pharmaguard.controller;

import com.pharmaguard.entity.AuditLog;
import com.pharmaguard.entity.User;
import com.pharmaguard.repository.UserRepository;
import com.pharmaguard.service.AuditLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class SecurityDashboardController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public SecurityDashboardController(UserRepository userRepository, AuditLogService auditLogService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping({"/security/dashboard", "/security"})
    public String viewSecurityDashboard(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "admin@pharmaguard.com";
        Optional<User> userOpt = userRepository.findByEmail(username);

        User currentUser = userOpt.orElseGet(() -> {
            User u = new User();
            u.setFullName("Admin User");
            u.setEmail(username);
            u.setRole("ADMIN");
            return u;
        });

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("username", currentUser.getFullName());

        List<AuditLog> recentLogs = auditLogService.getRecentLogs();
        model.addAttribute("recentLogs", recentLogs);

        return "security-dashboard";
    }

    @PostMapping("/security/change-password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";
        String email = principal.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Current password does not match.");
                return "redirect:/security/dashboard";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            auditLogService.logAction(user.getEmail(), "PASSWORD_CHANGE", "User", String.valueOf(user.getId()), "User changed account password.");
            redirectAttributes.addFlashAttribute("successMessage", "✓ Password updated successfully.");
        }

        return "redirect:/security/dashboard";
    }
}
