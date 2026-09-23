package com.schoolmanagement.identity;

import static org.springframework.http.HttpStatus.*;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public UserAccount get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(UNAUTHORIZED);
        try {
            return users
                .findById(Long.valueOf(auth.getName()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }
    }

    public UserAccount require(String... roles) {
        var user = get();
        for (String role : roles) if (user.has(role)) return user;
        throw new ResponseStatusException(FORBIDDEN, "Permission denied");
    }
}
