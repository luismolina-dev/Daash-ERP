package com.daash.erp.security;

import com.daash.erp.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final long ACCESS_TTL_SECONDS = 15L * 60L;
    public static final long REFRESH_TTL_SECONDS = 7L * 24L * 60L * 60L;

    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtService(@Value("${jwt.secret}") String accessSecret,
                      @Value("${refresh.secret}") String refreshSecret) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.email)
                .claim("role", user.role.value())
                .claim("userId", user.id)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_TTL_SECONDS * 1000L))
                .signWith(accessKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.email)
                .claim("role", user.role.value())
                .claim("userId", user.id)
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + REFRESH_TTL_SECONDS * 1000L))
                .signWith(refreshKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return parse(token, accessKey);
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token, refreshKey);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new JwtException("El token no es de refresco");
        }
        return claims;
    }

    private Claims parse(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
