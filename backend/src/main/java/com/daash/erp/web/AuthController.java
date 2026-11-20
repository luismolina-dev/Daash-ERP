package com.daash.erp.web;

import com.daash.erp.dto.AuthDtos.AuthResponse;
import com.daash.erp.dto.AuthDtos.LoginRequest;
import com.daash.erp.dto.UserDto;
import com.daash.erp.model.User;
import com.daash.erp.repository.UserRepository;
import com.daash.erp.security.JwtService;
import com.daash.erp.security.LoginRateLimiter;
import com.daash.erp.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refreshToken";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final AuditService auditService;
    private final boolean cookieSecure;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                          LoginRateLimiter rateLimiter, AuditService auditService,
                          @Value("${app.cookie-secure}") boolean cookieSecure) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest servletRequest) {
        String ip = clientIp(servletRequest);
        rateLimiter.check(ip, request.email());
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash)) {
            rateLimiter.recordFailure(ip, request.email());
            auditService.log("LOGIN_FALLIDO", "User", null, user == null ? null : user.id,
                    "Intento fallido desde " + ip, ip);
            throw new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas");
        }
        rateLimiter.clear(ip, request.email());
        auditService.log("LOGIN", "User", user.id.toString(), user.id, "Inicio de sesión", ip);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(jwtService.generateRefreshToken(user), 7L).toString())
                .body(authResponse(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "No hay cookie de refresco");
        }
        String email = jwtService.parseRefreshToken(refreshToken).getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado"));
        return ResponseEntity.ok(authResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        String email = AuthContext.currentEmail();
        Long userId = email == null ? null : userRepository.findByEmail(email).map(user -> user.id).orElse(null);
        auditService.log("LOGOUT", "User", userId == null ? null : userId.toString(), userId,
                "Cierre de sesión", clientIp(servletRequest));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0L).toString())
                .build();
    }

    @GetMapping("/me")
    public UserDto me() {
        String email = AuthContext.currentEmail();
        User user = email == null ? null : userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado"));
        if (user == null) {
            throw new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Autenticación requerida");
        }
        return UserDto.from(user);
    }

    private AuthResponse authResponse(User user) {
        return new AuthResponse(jwtService.generateAccessToken(user), "Bearer", JwtService.ACCESS_TTL_SECONDS,
                UserDto.from(user));
    }

    private ResponseCookie refreshCookie(String token, long days) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(days))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
