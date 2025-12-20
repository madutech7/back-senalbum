package com.senalbum.security;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.util.UUID;

/**
 * Utility class for security-related operations
 */
@Component
public class SecurityUtils {

    @Autowired
    private PhotographerRepository photographerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get the photographer ID from the current security context
     */
    @NonNull
    public UUID getCurrentPhotographerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            Photographer photographer = photographerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Photographer not found: " + email));
            return photographer.getId();
        }

        throw new RuntimeException("Unable to extract photographer ID from authentication");
    }

    /**
     * Get the photographer ID from a JWT token in the Authorization header
     * This is a fallback method for cases where we need to extract from the header
     * directly
     */
    public UUID getPhotographerIdFromToken(String token) {
        return jwtUtil.getPhotographerIdFromToken(token);
    }
}
