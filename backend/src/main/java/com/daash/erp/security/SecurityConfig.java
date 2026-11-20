package com.daash.erp.security;

import com.daash.erp.dto.ApiError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-origin}") String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException ex) -> {
            writeError(objectMapper, response, request, 401, "Unauthorized", "Autenticación requerida");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.access.AccessDeniedException ex) -> {
            writeError(objectMapper, response, request, 403, "Forbidden", "Rol insuficiente para este recurso");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers.addHeaderWriter((request, response) -> {
                    response.setHeader("Content-Security-Policy",
                            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self' data:; font-src 'self' data:; connect-src 'self'; "
                                    + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
                    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                    response.setHeader("X-Frame-Options", "DENY");
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    response.setHeader("Referrer-Policy", "no-referrer");
                }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAnyRole("ADMIN_BODEGA", "GERENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/products/*").hasAnyRole("ADMIN_BODEGA", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/inventory/kardex").hasAnyRole("ADMIN_BODEGA", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/inventory/adjustments").hasAnyRole("ADMIN_BODEGA", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/customers").hasAnyRole("CAJERO", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/customers").hasAnyRole("CAJERO", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/suppliers").hasAnyRole("ADMIN_BODEGA", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/suppliers").hasAnyRole("ADMIN_BODEGA", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/purchases").hasAnyRole("ADMIN_BODEGA", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/purchases").hasAnyRole("ADMIN_BODEGA", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/purchases/*/pdf")
                        .hasAnyRole("ADMIN_BODEGA", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/purchases/*/resend-email")
                        .hasAnyRole("ADMIN_BODEGA", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/cash-registers/current").hasAnyRole("CAJERO", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/cash-registers/open", "/api/cash-registers/close")
                        .hasAnyRole("CAJERO", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/sales/*/anular").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/sales").hasAnyRole("CAJERO", "GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/sales").hasAnyRole("CAJERO", "GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/dte/contingencies").hasAnyRole("GERENTE", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/dte/simulation/downtime").hasAnyRole("GERENTE", "AUDITOR")
                        .requestMatchers("/api/reports/**").hasAnyRole("GERENTE", "AUDITOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(ObjectMapper objectMapper, HttpServletResponse response, HttpServletRequest request,
                            int status, String error, String message) {
        try {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    new ApiError(Instant.now(), status, error, message, request.getRequestURI()));
        } catch (Exception ignored) {
            // El contenedor se encargará de la respuesta si no se puede escribir el JSON.
        }
    }
}
