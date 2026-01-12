package com.webcrawler.backend.service;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.repository.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getByUsername(String username) {
        return appUserRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
