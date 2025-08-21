package com.backendev.userservice.security.jwt;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.exception.TokenExpiredException;
import com.backendev.userservice.service.AuditService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtService {

    private final AuditService auditService;
    private static final long TOKEN_EXPIRATION_TIME = 900000;

    @Value("${JWT_SECRET}")
    private String secretKey;

    public JwtService(AuditService auditService) {
        this.auditService = auditService;
    }

    public String generateToken(String username, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_TIME))
                .signWith(getSignedKey())
                .compact();
    }

    private Key getSignedKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //Get username from token - JWT Filter
    public String extractUsername(String token){
        Claims claims = getClaims(token);
        if (claims == null) {
            return null; // Token is expired or invalid
        }
        return claims.getSubject();
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getSignedKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            String email = e.getClaims() != null ? e.getClaims().getSubject() : "unknown";
            auditService.auditLog(AuditEventType.TOKEN_EXPIRED, email, "Token expired");
            log.error("JWT expired.");
            throw new TokenExpiredException("JWT expired. Please log in again.", e);
        }
    }

    // Validate JWT Token
        public boolean validateToken(String token) {
            try {
                return !isTokenExpired(token);
            } catch (Exception e) {
                return false;
            }
        }

    private boolean isTokenExpired(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration().before(new Date());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String username = claims.getSubject();

        List<String> roles = claims.get("roles", List.class);
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails principal = new User(username, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

}
