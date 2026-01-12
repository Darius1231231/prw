package com.webcrawler.backend.service;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.repository.AppUserRepository;
import com.webcrawler.backend.security.JwtService;
import com.webcrawler.backend.web.dto.AuthRequest;
import com.webcrawler.backend.web.dto.AuthResponse;
import com.webcrawler.backend.web.dto.RegisterRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthenticationManager authenticationManager
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of("USER"));
        appUserRepository.save(user);

        return generateAuthResponse(user);
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateAuthResponse(userDetails);
    }

    private AuthResponse generateAuthResponse(UserDetails userDetails) {
        Set<String> roles = userDetails.getAuthorities().stream()
            .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
            .collect(java.util.stream.Collectors.toSet());

        String token = jwtService.generateToken(userDetails, Map.of("roles", roles));
        Instant expiresAt = jwtService.extractExpiration(token);

        return new AuthResponse(token, expiresAt, userDetails.getUsername(), roles);
    }
}
