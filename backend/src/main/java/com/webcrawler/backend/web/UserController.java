package com.webcrawler.backend.web;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.service.UserService;
import com.webcrawler.backend.web.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        String username = authentication.getName();
        AppUser user = userService.getByUsername(username);
        return new UserResponse(user.getId(), user.getUsername(), user.getRoles(), user.getCreatedAt());
    }
}
