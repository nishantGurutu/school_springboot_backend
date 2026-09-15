package com.school.management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final long ACCESS_EXPIRATION = 1000 * 60 * 30;        // 30 minutes
    private static final long REFRESH_EXPIRATION = 1000 * 60 * 60 * 24 * 7; // 7 days

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret:super-secret-key-super-secret-key-super-secret-key}") String secret) {
        String normalized = secret == null || secret.length() < 32
                ? "super-secret-key-super-secret-key-super-secret-key"
                : secret;
        this.key = Keys.hmacShaKeyFor(normalized.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email) {
        return buildToken(email, ACCESS_EXPIRATION, false);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, REFRESH_EXPIRATION, true);
    }

    public TokenPair generateTokenPair(String email) {
        return new TokenPair(generateAccessToken(email), generateRefreshToken(email),
                System.currentTimeMillis() + ACCESS_EXPIRATION);
    }

    private String buildToken(String email, long expiration, boolean refresh) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", refresh ? "refresh" : "access");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isRefreshToken(String token) {
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return "refresh".equals(type);
    }

    public boolean isAccessToken(String token) {
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return "access".equals(type);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername())
                && !extractExpiration(token).before(new Date())
                && isAccessToken(token);
    }

    public boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp == null || exp.before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            return null;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return claims == null ? null : resolver.apply(claims);
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresAt) {}
}