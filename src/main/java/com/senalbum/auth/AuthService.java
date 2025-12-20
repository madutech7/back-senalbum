package com.senalbum.auth;

import com.senalbum.auth.dto.AuthResponse;
import com.senalbum.auth.dto.LoginRequest;
import com.senalbum.auth.dto.RegisterRequest;
import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import com.senalbum.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service d'authentification
 */
@Service
public class AuthService {

    @Autowired
    private PhotographerRepository photographerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (photographerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Photographer photographer = new Photographer();
        photographer.setEmail(request.getEmail());
        photographer.setPassword(passwordEncoder.encode(request.getPassword()));
        photographer.setFirstName(request.getFirstName());
        photographer.setLastName(request.getLastName());

        // Generate default avatar if names provided, otherwise use email initial logic
        // later in frontend or server side
        String fullName = (request.getFirstName() != null ? request.getFirstName() : "") + "+" +
                (request.getLastName() != null ? request.getLastName() : "User");
        if (fullName.equals("+User"))
            fullName = request.getEmail();

        photographer.setProfilePictureUrl("https://ui-avatars.com/api/?name=" + fullName + "&background=random");

        photographer = photographerRepository.save(photographer);

        String token = jwtUtil.generateToken(photographer.getId(), photographer.getEmail());
        return new AuthResponse(token, photographer.getEmail(), photographer.getFirstName(), photographer.getLastName(),
                photographer.getProfilePictureUrl());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        Photographer photographer = photographerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Photographer not found"));

        String token = jwtUtil.generateToken(photographer.getId(), photographer.getEmail());
        return new AuthResponse(token, photographer.getEmail(), photographer.getFirstName(), photographer.getLastName(),
                photographer.getProfilePictureUrl());
    }
}
