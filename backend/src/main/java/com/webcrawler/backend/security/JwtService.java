package com.webcrawler.backend.security;

import com.webcrawler.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private Key signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        this.signingKey = buildKey(properties.getSecret());
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getExpiration());

        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration().toInstant());
    }

    public <T> T extractClaim(String token, Function<Claims, T> extractor) {
        Claims claims = parseAllClaims(token);
        return extractor.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key buildKey(String secret) {
        return Optional.ofNullable(secret)
            .map(value -> value.getBytes(StandardCharsets.UTF_8))
            .map(bytes -> {
                if (bytes.length < 32) {
                    throw new IllegalStateException("JWT secret must be at least 32 characters long");
                }
                return Keys.hmacShaKeyFor(bytes);
            })
            .orElseThrow(() -> new IllegalStateException("JWT secret must be configured"));
    }
}
