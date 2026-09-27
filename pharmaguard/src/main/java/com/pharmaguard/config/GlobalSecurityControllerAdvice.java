package com.pharmaguard.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalSecurityControllerAdvice {

    @ModelAttribute("username")
    public String populateUsername(Principal principal) {
        return principal != null ? principal.getName() : "Guest";
    }

    @ModelAttribute("userRole")
    public String populateRole(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        return "GUEST";
    }

    @ModelAttribute("isAdmin")
    public boolean populateIsAdmin(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}
