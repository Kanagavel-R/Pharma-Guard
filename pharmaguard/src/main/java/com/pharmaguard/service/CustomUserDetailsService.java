package com.pharmaguard.service;

import com.pharmaguard.entity.User;
import com.pharmaguard.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        String search = username.trim().toLowerCase();
        User user = userRepository.findByUsernameOrEmail(search, search)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + username));

        String role = user.getRole() != null ? user.getRole().toUpperCase() : "STAFF";
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername() != null ? user.getUsername() : user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
