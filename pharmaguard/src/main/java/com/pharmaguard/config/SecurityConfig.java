package com.pharmaguard.config;

import com.pharmaguard.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**", "/medicines/api/**", "/recalls/**", "/security/**", "/reports/**", "/staff/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(cto -> {})
                .xssProtection(xss -> {})
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .sessionManagement(session -> session
                .maximumSessions(2)
                .expiredUrl("/login?expired=true")
            )
            .authorizeHttpRequests(auth -> auth
                // Public Static Assets, Landing Page, Login/Signup & Health Check
                .requestMatchers("/", "/login", "/signup", "/api/health/**", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/h2-console/**").permitAll()
                // Role-restricted routes
                .requestMatchers("/medicines/delete/**", "/suppliers/delete/**", "/api/medicine/delete/**", "/users/delete/**", "/settings/reset").hasRole("ADMIN")
                .requestMatchers("/medicines/add", "/medicines/edit/**", "/suppliers/add", "/suppliers/edit/**", "/suppliers/save", "/suppliers/update", "/transactions/add").hasAnyRole("ADMIN", "PHARMACIST", "STAFF")
                // Core authenticated endpoints
                .requestMatchers("/dashboard", "/ai-assistant", "/medicines/**", "/inventory/**", "/orders/**", "/alerts/**", "/transactions/**", "/suppliers/**", "/scan/**", "/verifications/**", "/verification/**", "/settings/**", "/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
