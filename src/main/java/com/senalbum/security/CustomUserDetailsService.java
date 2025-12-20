package com.senalbum.security;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Service pour charger les détails de l'utilisateur depuis la base de données
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private PhotographerRepository photographerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Photographer photographer = photographerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Photographer not found: " + email));

        return new User(photographer.getEmail(), photographer.getPassword(), new ArrayList<>());
    }
}
