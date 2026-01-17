package com.example.SecurityAdvance.security.jwt;

import com.example.SecurityAdvance.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    // Tao Token
    public String generateToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();

        List<String> roles = userDetails.getUser()
                .getUserRoles()
                .stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getUser().getId());
        claims.put("role", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expiration)))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

    }

    // Tao key
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Lay ra tat ca thong tin tu token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Trich xuat ra thong tin cua 1 claim
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    // Lay thong tin cua username
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Validation Token
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}
