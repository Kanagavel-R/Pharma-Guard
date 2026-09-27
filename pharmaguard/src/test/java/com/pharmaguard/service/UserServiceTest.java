package com.pharmaguard.service;

import com.pharmaguard.entity.User;
import com.pharmaguard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void testRegisterUser_Success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerUser("johndoe", "John Doe", "john@example.com", "Secret123!", "STAFF");

        assertNotNull(user);
        assertEquals("John Doe", user.getFullName());
        assertEquals("john@example.com", user.getEmail());
        assertNotEquals("Secret123!", user.getPassword());
        assertTrue(passwordEncoder.matches("Secret123!", user.getPassword()));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_DuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                userService.registerUser("johndoe", "John Doe", "john@example.com", "Secret123!", "STAFF"));
    }

    @Test
    void testAuthenticateUser_Success() {
        User existingUser = new User();
        existingUser.setFullName("Jane Smith");
        existingUser.setEmail("jane@example.com");
        existingUser.setUsername("janesmith");
        existingUser.setPassword(passwordEncoder.encode("Password123!"));

        when(userRepository.findByUsernameOrEmail("janesmith", "janesmith"))
                .thenReturn(Optional.of(existingUser));

        Optional<User> authOpt = userService.findByUsername("janesmith");

        assertTrue(authOpt.isPresent());
        assertTrue(userService.validatePassword(authOpt.get(), "Password123!"));
    }

    @Test
    void testAuthenticateUser_WrongPassword() {
        User existingUser = new User();
        existingUser.setFullName("Jane Smith");
        existingUser.setEmail("jane@example.com");
        existingUser.setUsername("janesmith");
        existingUser.setPassword(passwordEncoder.encode("Password123!"));

        when(userRepository.findByUsernameOrEmail("janesmith", "janesmith"))
                .thenReturn(Optional.of(existingUser));

        Optional<User> authOpt = userService.findByUsername("janesmith");

        assertTrue(authOpt.isPresent());
        assertFalse(userService.validatePassword(authOpt.get(), "WrongPass"));
    }
}
