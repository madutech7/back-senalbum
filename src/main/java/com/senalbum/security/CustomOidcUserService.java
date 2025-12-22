package com.senalbum.security;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomOidcUserService extends OidcUserService {

  @Autowired
  private PhotographerRepository photographerRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    String email = oidcUser.getAttribute("email");

    if (email != null) {
      Photographer photographer = photographerRepository.findByEmailIgnoreCase(email)
          .orElseGet(() -> {
            Photographer newUser = new Photographer();
            newUser.setEmail(email);
            newUser.setFirstName(oidcUser.getAttribute("given_name"));
            newUser.setLastName(oidcUser.getAttribute("family_name"));
            newUser.setProfilePictureUrl(oidcUser.getAttribute("picture"));
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            return newUser;
          });

      photographer.setEnabled(true);
      photographerRepository.save(photographer);
    }

    return oidcUser;
  }
}
