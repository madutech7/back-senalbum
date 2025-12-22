package com.senalbum.auth;

import com.senalbum.auth.dto.AuthResponse;
import com.senalbum.auth.dto.LoginRequest;
import com.senalbum.auth.dto.RegisterRequest;
import com.senalbum.auth.dto.VerificationRequest;
import com.senalbum.email.EmailService;
import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import com.senalbum.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

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

    @Autowired
    private EmailService emailService;

    private static final int VERIFICATION_CODE_EXPIRATION_MINUTES = 15;

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

        // Set verification code
        String verificationCode = generateVerificationCode();
        photographer.setVerificationCode(verificationCode);
        photographer
                .setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRATION_MINUTES));
        photographer.setEnabled(false);

        photographer = photographerRepository.save(photographer);

        // Send email (Async)
        emailService.sendVerificationEmail(photographer.getEmail(), verificationCode);

        return AuthResponse.pending(photographer.getEmail());
    }

    public AuthResponse verifyCode(VerificationRequest request) {
        Photographer photographer = photographerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (photographer.isEnabled()) {
            throw new RuntimeException("Compte déjà activé");
        }

        if (photographer.getVerificationCode() == null
                || !photographer.getVerificationCode().equals(request.getCode())) {
            throw new RuntimeException("Code de confirmation invalide");
        }

        if (photographer.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Code de confirmation expiré");
        }

        photographer.setEnabled(true);
        photographer.setVerificationCode(null);
        photographer.setVerificationCodeExpiresAt(null);
        photographerRepository.save(photographer);

        String token = jwtUtil.generateToken(photographer.getId(), photographer.getEmail());
        return new AuthResponse(token, photographer.getEmail(), photographer.getFirstName(), photographer.getLastName(),
                photographer.getProfilePictureUrl());
    }

    public void resendCode(String email) {
        Photographer photographer = photographerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (photographer.isEnabled()) {
            throw new RuntimeException("Compte déjà activé");
        }

        String verificationCode = generateVerificationCode();
        photographer.setVerificationCode(verificationCode);
        photographer
                .setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRATION_MINUTES));
        photographerRepository.save(photographer);

        emailService.sendVerificationEmail(photographer.getEmail(), verificationCode);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        Photographer photographer = photographerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Photographer not found"));

        if (!photographer.isEnabled()) {
            throw new RuntimeException("Votre compte n'est pas encore activé. Veuillez vérifier votre boîte mail.");
        }

        String token = jwtUtil.generateToken(photographer.getId(), photographer.getEmail());
        return new AuthResponse(token, photographer.getEmail(), photographer.getFirstName(), photographer.getLastName(),
                photographer.getProfilePictureUrl());
    }
}
