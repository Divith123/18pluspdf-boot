package com.chnindia.eighteenpluspdf.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    
    @Value("${app.security.jwt-secret:default-jwt-secret-key-for-development-only-12345678901234567890}")
    private String jwtSecret;
    
    @Value("${app.security.jwt-expiration:86400000}")
    private long jwtExpiration;
    
    public String generateToken(String username, String apiKey) {
        return generateToken(username, apiKey, new HashMap<>());
    }
    
    public String generateToken(String username, String apiKey, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        claims.put("apiKey", apiKey);
        
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    public String extractUsername(String token) {
        return parseJwt(token).getSubject();
    }
    
    public boolean isTokenValid(String token) {
        try {
            parseJwt(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private io.jsonwebtoken.Claims parseJwt(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}