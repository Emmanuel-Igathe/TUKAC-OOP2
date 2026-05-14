package com.tukac.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/", "/index.html", "/login.html", "/home.html", "/*.html", "/css/**", "/js/**", "/favicon.ico", "/logo.svg").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/about").authenticated()

                // Admin roles — user management & audit logs
                .requestMatchers("/api/users/**", "/api/admin/logs", "/api/admin/logs/**").hasAnyRole("CHAIRPERSON", "VICE-CHAIRPERSON")

                // GET requests — any authenticated user can read
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/blog", "/api/blog/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/forum", "/api/forum/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/transactions").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").authenticated()

                // All other API calls — authenticated (method-level @PreAuthorize handles role checks)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
