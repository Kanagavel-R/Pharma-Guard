package com.pharmaguard.controller;

import com.pharmaguard.entity.User;
import com.pharmaguard.service.AlertService;
import com.pharmaguard.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class ProfileController {

    private final UserService userService;
    private final AlertService alertService;
    private final com.pharmaguard.service.SettingsService settingsService;

    public ProfileController(UserService userService, AlertService alertService, com.pharmaguard.service.SettingsService settingsService) {
        this.userService = userService;
        this.alertService = alertService;
        this.settingsService = settingsService;
    }

    @GetMapping("/profile")
    public String showProfilePage(Principal principal, Model model) {
        String username = (principal != null) ? principal.getName() : "admin";
        Optional<User> currentUserOpt = userService.findByUsername(username);

        User currentUser = currentUserOpt.orElseGet(() -> {
            User dummy = new User();
            dummy.setUsername(username);
            dummy.setFullName("Administrator");
            dummy.setEmail(username + "@pharmaguard.com");
            dummy.setRole("ADMIN");
            dummy.setPhoneNumber("+1 800-555-0199");
            return dummy;
        });

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("userFullName", currentUser.getFullName());
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("userRole", currentUser.getRole());
        model.addAttribute("userInitials", currentUser.getInitials());
        model.addAttribute("pharmacySettings", settingsService.getSettings());
        model.addAttribute("activeAlertCount", alertService.getActiveAlertCount());

        return "profile";
    }

    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProfileApi(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "admin";
        Optional<User> currentUserOpt = userService.findByUsername(username);

        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = currentUserOpt.get();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("id", user.getId());
        profileData.put("username", user.getUsername());
        profileData.put("fullName", user.getFullName());
        profileData.put("firstName", user.getFirstName());
        profileData.put("lastName", user.getLastName());
        profileData.put("email", user.getEmail());
        profileData.put("phoneNumber", user.getPhoneNumber());
        profileData.put("pharmacyName", user.getPharmacyName());
        profileData.put("employeeId", user.getEmployeeId());
        profileData.put("role", user.getRole());
        profileData.put("displayRole", user.getDisplayRole());
        profileData.put("initials", user.getInitials());
        profileData.put("profilePhoto", user.getProfilePhoto());
        profileData.put("enabled", user.isEnabled());
        profileData.put("accountStatusText", user.getAccountStatusText());
        profileData.put("createdAt", user.getCreatedAt());
        profileData.put("lastLogin", user.getLastLogin());

        return ResponseEntity.ok(profileData);
    }

    @PostMapping("/profile/update")
    public String updateProfileForm(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "pharmacyName", required = false) String pharmacyName,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String username = (principal != null) ? principal.getName() : "admin";
            String computedName = fullName;
            if ((computedName == null || computedName.isBlank()) && (firstName != null || lastName != null)) {
                computedName = ((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim();
            }

            userService.updateProfile(username, computedName, email, phoneNumber, pharmacyName);
            redirectAttributes.addFlashAttribute("successMsg", "✓ Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "⚠️ Failed to update profile: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfileApi(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            String username = (authentication != null) ? authentication.getName() : "admin";
            String fullName = payload.get("fullName");
            String firstName = payload.get("firstName");
            String lastName = payload.get("lastName");
            String email = payload.get("email");
            String phoneNumber = payload.get("phoneNumber");
            String pharmacyName = payload.get("pharmacyName");

            String computedName = fullName;
            if ((computedName == null || computedName.isBlank()) && (firstName != null || lastName != null)) {
                computedName = ((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim();
            }

            User updated = userService.updateProfile(username, computedName, email, phoneNumber, pharmacyName);

            Map<String, Object> uMap = new HashMap<>();
            uMap.put("id", updated.getId());
            uMap.put("fullName", updated.getFullName());
            uMap.put("firstName", updated.getFirstName());
            uMap.put("lastName", updated.getLastName());
            uMap.put("email", updated.getEmail());
            uMap.put("phoneNumber", updated.getPhoneNumber() != null ? updated.getPhoneNumber() : "");
            uMap.put("pharmacyName", updated.getPharmacyName() != null ? updated.getPharmacyName() : "");
            uMap.put("employeeId", updated.getEmployeeId());
            uMap.put("role", updated.getRole());
            uMap.put("displayRole", updated.getDisplayRole());
            uMap.put("initials", updated.getInitials());
            uMap.put("profilePhoto", updated.getProfilePhoto() != null ? updated.getProfilePhoto() : "");

            response.put("success", true);
            response.put("message", "✓ Profile updated successfully.");
            response.put("user", uMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/profile/photo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            String username = (authentication != null) ? authentication.getName() : "admin";
            String photoData = payload.get("photoData");

            if (photoData == null || photoData.isBlank()) {
                response.put("success", false);
                response.put("message", "Please select a valid image file.");
                return ResponseEntity.badRequest().body(response);
            }

            User updated = userService.updateProfilePhoto(username, photoData);

            response.put("success", true);
            response.put("message", "✓ Profile photo updated successfully.");
            response.put("profilePhoto", updated.getProfilePhoto());
            response.put("initials", updated.getInitials());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to update profile photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/profile/photo/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removePhoto(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (authentication != null) ? authentication.getName() : "admin";
            User updated = userService.updateProfilePhoto(username, null);

            response.put("success", true);
            response.put("message", "✓ Profile photo removed successfully.");
            response.put("initials", updated.getInitials());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to remove profile photo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/pharmacy/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePharmacyApi(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        try {
            com.pharmaguard.entity.PharmacySettings current = settingsService.getSettings();

            if (payload.containsKey("pharmacyName") && !payload.get("pharmacyName").isBlank()) {
                current.setPharmacyName(payload.get("pharmacyName").trim());
            }
            if (payload.containsKey("pharmacyId")) {
                current.setPharmacyId(payload.get("pharmacyId").trim());
            }
            if (payload.containsKey("address")) {
                current.setAddress(payload.get("address").trim());
            }
            if (payload.containsKey("phone")) {
                current.setPhone(payload.get("phone").trim());
            }
            if (payload.containsKey("email")) {
                current.setEmail(payload.get("email").trim());
            }

            if (payload.containsKey("monHours")) current.setMonHours(payload.get("monHours"));
            if (payload.containsKey("tueHours")) current.setTueHours(payload.get("tueHours"));
            if (payload.containsKey("wedHours")) current.setWedHours(payload.get("wedHours"));
            if (payload.containsKey("thuHours")) current.setThuHours(payload.get("thuHours"));
            if (payload.containsKey("friHours")) current.setFriHours(payload.get("friHours"));
            if (payload.containsKey("satHours")) current.setSatHours(payload.get("satHours"));
            if (payload.containsKey("sunHours")) current.setSunHours(payload.get("sunHours"));

            com.pharmaguard.entity.PharmacySettings saved = settingsService.saveSettings(current);

            // Also sync user pharmacyName if authenticated
            if (authentication != null) {
                try {
                    userService.updateProfile(authentication.getName(), null, null, null, saved.getPharmacyName());
                } catch (Exception ignored) {}
            }

            response.put("success", true);
            response.put("message", "✓ Pharmacy information updated successfully.");
            response.put("pharmacy", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to update pharmacy information: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping({"/api/settings/notifications/update", "/api/settings/notifications"})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateNotificationPreferencesApi(
            @RequestBody Map<String, Boolean> payload) {

        Map<String, Object> response = new HashMap<>();
        try {
            com.pharmaguard.entity.PharmacySettings current = settingsService.getSettings();

            if (payload.containsKey("expiryAlerts")) current.setExpiryAlerts(payload.get("expiryAlerts"));
            if (payload.containsKey("lowStockAlerts")) current.setLowStockAlerts(payload.get("lowStockAlerts"));
            if (payload.containsKey("verificationAlerts")) current.setVerificationAlerts(payload.get("verificationAlerts"));
            if (payload.containsKey("supplierAlerts")) current.setSupplierAlerts(payload.get("supplierAlerts"));
            if (payload.containsKey("securityNotifications")) current.setSecurityNotifications(payload.get("securityNotifications"));
            if (payload.containsKey("emailNotifications")) current.setEmailNotifications(payload.get("emailNotifications"));
            if (payload.containsKey("inAppNotifications")) current.setInAppNotifications(payload.get("inAppNotifications"));

            com.pharmaguard.entity.PharmacySettings saved = settingsService.saveSettings(current);

            response.put("success", true);
            response.put("message", "✓ Notification preferences saved successfully.");
            response.put("settings", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unable to save preferences: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
