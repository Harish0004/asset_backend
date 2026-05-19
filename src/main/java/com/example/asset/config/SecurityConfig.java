package com.example.asset.config;

import com.example.asset.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central security configuration: stateless JWT auth with role-based URL authorization
 * and CORS for frontend clients.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight must not require authentication
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public auth endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()

                        // ADMIN — full platform management
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Assets — write operations (ADMIN only)
                        .requestMatchers(HttpMethod.POST, "/api/assets", "/api/assets/").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/assets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/assets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/assets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/assets/**").hasRole("ADMIN")

                        // Assets — assignment & maintenance (ADMIN + TECHNICIAN)
                        .requestMatchers("/api/assets/*/assign", "/api/assets/*/assign/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers("/api/assets/*/maintenance", "/api/assets/*/maintenance/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN")

                        // Assets — read (all roles)
                        .requestMatchers(HttpMethod.GET, "/api/assets", "/api/assets/")
                                .hasAnyRole("ADMIN", "TECHNICIAN", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/assets/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN", "EMPLOYEE")

                        // Assignment history (ADMIN + TECHNICIAN)
                        .requestMatchers("/api/assignments/**").hasAnyRole("ADMIN", "TECHNICIAN")

                        // Tickets — lifecycle updates (ADMIN + TECHNICIAN)
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasRole("ADMIN")

                        // Tickets — create & read (all roles)
                        .requestMatchers("/api/tickets/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN", "EMPLOYEE")

                        // Notifications (all authenticated roles)
                        .requestMatchers("/api/notifications/**")
                                .hasAnyRole("ADMIN", "TECHNICIAN", "EMPLOYEE")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS for local frontends. Uses allowedOriginPatterns (not allowedOrigins("*"))
     * so allowCredentials can remain true for cookie-based flows.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
