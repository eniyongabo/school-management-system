package com.schoolmanagement.identity;

import static com.schoolmanagement.identity.IdentityDtos.*;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IdentityController {

    private final IdentityService service;
    private final CurrentUser current;
    private final UserRepository users;

    public IdentityController(IdentityService s, CurrentUser c, UserRepository u) {
        service = s;
        current = c;
        users = u;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody Register r) {
        return service.register(r);
    }

    @PostMapping("/auth/login")
    public Session login(@Valid @RequestBody Login r) {
        return service.login(r);
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout() {
        service.logout();
        return Map.of("message", "Signed out");
    }

    @GetMapping("/auth/me")
    public UserView me() {
        return UserView.of(current.get());
    }

    @PutMapping("/auth/profile")
    public UserView profile(@Valid @RequestBody Profile p) {
        return service.profile(p);
    }

    @PostMapping("/auth/change-password")
    public Map<String, String> password(@Valid @RequestBody Password p) {
        service.password(p);
        return Map.of("message", "Password changed. Sign in again.");
    }

    @PostMapping("/auth/forgot-password")
    public Map<String, String> forgot(@Valid @RequestBody EmailRequest p) {
        service.forgot(p.email());
        return Map.of("message", "If eligible, an email has been sent.");
    }

    @PostMapping("/auth/resend-verification")
    public Map<String, String> resend(@Valid @RequestBody EmailRequest p) {
        service.resend(p.email());
        return Map.of("message", "If eligible, an email has been sent.");
    }

    @PostMapping("/auth/reset-password")
    public Map<String, String> reset(@Valid @RequestBody Reset p) {
        service.reset(p);
        return Map.of("message", "Password reset. Sign in again.");
    }

    @PostMapping("/auth/verify-email")
    public Map<String, String> verify(@Valid @RequestBody Verify p) {
        service.verify(p.token());
        return Map.of("message", "Email verified. You may sign in.");
    }

    @GetMapping("/users")
    public Page<UserView> users(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "") String search
    ) {
        current.require("ADMINISTRATOR");
        return users.search(search, PageRequest.of(Math.max(0, page), 25, Sort.by("id"))).map(UserView::of);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody AdminAccount p) {
        current.require("ADMINISTRATOR");
        return UserView.of(
            service.create(p.email(), p.password(), p.firstName(), p.lastName(), p.role(), true)
        );
    }

    @PutMapping("/users/{id}/access")
    public UserView access(@PathVariable long id, @Valid @RequestBody Access p) {
        return service.access(id, p);
    }
}
