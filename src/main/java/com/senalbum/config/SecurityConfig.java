package com.senalbum.config;

import com.senalbum.security.JwtAuthenticationFilter;
import com.senalbum.security.CustomOAuth2UserService;
import com.senalbum.security.CustomOidcUserService;
import com.senalbum.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration de sécurité Spring Security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private CustomOAuth2UserService customOAuth2UserService;

        @Autowired
        private CustomOidcUserService customOidcUserService;

        @Autowired
        private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Endpoints publics
                                                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**")
                                                .permitAll()
                                                .requestMatchers("/api/public/**").permitAll()
                                                .requestMatchers("/api/admin/**").permitAll()
                                                .requestMatchers("/api/mobile/**").permitAll()
                                                .requestMatchers("/api/storage/**").permitAll()
                                                // Tous les autres endpoints nécessitent une authentification
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService)
                                                                .oidcUserService(customOidcUserService))
                                                .successHandler(oAuth2LoginSuccessHandler));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // Autorise toutes les origines en développement pour Expo et Angular
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
                configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With",
                                "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
                configuration.setExposedHeaders(
                                Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

}
