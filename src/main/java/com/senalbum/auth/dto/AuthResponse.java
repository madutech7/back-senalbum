package com.senalbum.auth.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private boolean requiresVerification;

    public AuthResponse(String token, String email, String firstName, String lastName, String profilePictureUrl) {
        this.token = token;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePictureUrl = profilePictureUrl;
        this.requiresVerification = false;
    }

    public static AuthResponse pending(String email) {
        AuthResponse response = new AuthResponse(null, email, null, null, null);
        response.setRequiresVerification(true);
        return response;
    }
}
