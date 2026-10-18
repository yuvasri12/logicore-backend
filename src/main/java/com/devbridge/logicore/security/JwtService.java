package com.devbridge.logicore.security;

import com.devbridge.logicore.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final Key key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration-minutes:480}") long expirationMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(Long id, String email, String fullName, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000L);
        return Jwts.builder()
                .setSubject(email)
                .claim("uid", id)
                .claim("name", fullName)
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getExpiresInSeconds() {
        return expirationMinutes * 60;
    }

    /** Returns the user if the token is valid and not expired; otherwise empty. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            Number uid = claims.get("uid", Number.class);
            String email = claims.getSubject();
            String name = claims.get("name", String.class);
            String roleName = claims.get("role", String.class);
            if (uid == null || email == null || name == null || roleName == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(uid.longValue(), email, name, Role.valueOf(roleName)));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected token: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}