package com.backendev.transactionservice.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService {

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            // The principal is usually the username/userId
            return authentication.getName();
        }

        return authentication.getName();
    }

    public String getCurrentUsername() {
        return getCurrentUserId(); // Same thing in most cases
    }
}
