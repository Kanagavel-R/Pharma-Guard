package com.pharmaguard.controller;

import com.pharmaguard.entity.User;
import com.pharmaguard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/signup")
    public String showSignupPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match. Please try again.");
            return "signup";
        }

        if (password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters long.");
            return "signup";
        }

        String computedFullName = fullName;
        if ((computedFullName == null || computedFullName.isBlank()) && (firstName != null || lastName != null)) {
            computedFullName = ((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim();
        }
        if (computedFullName == null || computedFullName.isBlank()) {
            computedFullName = email;
        }

        try {
            userService.registerUser(email, computedFullName, email, password, "STAFF");
            return "redirect:/login?success=Account+Created+Successfully";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        } catch (Exception e) {
            model.addAttribute("error", "An unexpected error occurred during account creation. Please try again.");
            return "signup";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
