package com.senalbum.security;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  @Autowired
  private PhotographerRepository photographerRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String email = oAuth2User.getAttribute("email");
    if (email == null) {
      throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
    }

    // Check if user exists, otherwise create
    Photographer photographer = photographerRepository.findByEmailIgnoreCase(email)
        .orElseGet(() -> {
          Photographer newUser = new Photographer();
          newUser.setEmail(email);
          newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
          return newUser;
        });

    // Always enable if coming from OAuth2 (Google already verified the email)
    photographer.setEnabled(true);
    photographerRepository.save(photographer);

    return oAuth2User;
  }
}
