package com.senalbum.security;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private PhotographerRepository photographerRepository;

  // Default to localhost:4200 if not set, taking the first valid origin usually
  private String frontendUrl = "http://localhost:4200";

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");

    Photographer photographer = photographerRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

    String token = jwtUtil.generateToken(photographer.getId(), photographer.getEmail());

    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
        .queryParam("token", token)
        .queryParam("email", email);

    if (photographer.getFirstName() != null)
      builder.queryParam("firstName", photographer.getFirstName());
    if (photographer.getLastName() != null)
      builder.queryParam("lastName", photographer.getLastName());
    if (photographer.getProfilePictureUrl() != null)
      builder.queryParam("profilePictureUrl", photographer.getProfilePictureUrl());

    String targetUrl = builder.build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
