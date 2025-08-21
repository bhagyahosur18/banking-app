package com.backendev.userservice.security.jwt;

import com.backendev.userservice.security.AppUserServiceDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserServiceDetails userServiceDetails;

    public JwtFilter(JwtService jwtService, AppUserServiceDetails userServiceDetails) {
        this.jwtService = jwtService;
        this.userServiceDetails = userServiceDetails;
    }

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Bypass JWT filter for Swagger-related endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/swagger-resources") || path.startsWith("/configuration")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        log.info("AUTHORIZATION HEADER {}", authHeader);
        String token = null;
        String username = null;
        if(authHeader != null && authHeader.startsWith("Bearer ")){
            token = authHeader.substring(7);
            log.info("Extracted token from JWT Filter {}", token);
            username = jwtService.extractUsername(token);
            log.info("Extracted username {}", username);
            log.info("Authentication object {}", SecurityContextHolder.getContext().getAuthentication());
        }
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = userServiceDetails.loadUserByUsername(username);
            if(jwtService.validateToken(token)){
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                log.info("Authentication in JWT filter {}", authToken);
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("SecurityContext Authentication after setting: {}", SecurityContextHolder.getContext().getAuthentication());
            }

        }

        filterChain.doFilter(request, response);
    }
}
