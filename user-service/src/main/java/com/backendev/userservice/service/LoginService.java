package com.backendev.userservice.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.dto.AuthRequest;
import com.backendev.userservice.dto.AuthResponse;
import com.backendev.userservice.security.AppUserDetails;
import com.backendev.userservice.security.jwt.JwtService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthResponse login(AuthRequest authRequest) {
        AppUserDetails userDetails = authenticate(authRequest);
        Map<String, Object> claims = buildClaims(userDetails);
        String token = jwtService.generateToken(userDetails.getUsername(), claims);

        auditService.auditLog(AuditEventType.LOGIN_SUCCESS, authRequest.getEmail(), "Login successful");
        return new AuthResponse(token);
    }

    private AppUserDetails authenticate(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        if (!authentication.isAuthenticated()) {
            log.error("Login Service: ", "Failed to authenticate");
            throw new BadCredentialsException("Invalid credentials");
        }
        return (AppUserDetails) authentication.getPrincipal();
    }

    private Map<String, Object> buildClaims(AppUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getId());
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        return claims;
    }
}
