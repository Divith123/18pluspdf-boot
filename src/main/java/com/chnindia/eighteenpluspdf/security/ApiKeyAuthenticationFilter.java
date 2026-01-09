package com.chnindia.eighteenpluspdf.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    @Value("${app.security.api-key-enabled:true}")
    private boolean apiKeyEnabled;
    
    @Value("${app.security.api-keys:dev-api-key-12345}")
    private String apiKeysConfig;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        if (!apiKeyEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String apiKeyHeader = request.getHeader("X-API-Key");
        final String apiKeyParam = request.getParameter("api_key");
        
        String apiKey = apiKeyHeader != null ? apiKeyHeader : apiKeyParam;
        
        if (apiKey == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Validate API key - simple comma-separated list
        List<String> validKeys = Arrays.asList(apiKeysConfig.split(","));
        boolean isValid = validKeys.stream()
            .map(String::trim)
            .anyMatch(key -> key.equals(apiKey));
        
        if (isValid) {
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    "api-user", 
                    null, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_API"))
                );
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.info("API Key authentication successful");
        } else {
            logger.warn("Invalid API key provided");
        }
        
        filterChain.doFilter(request, response);
    }
}