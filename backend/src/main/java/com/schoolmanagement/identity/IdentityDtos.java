package com.schoolmanagement.identity;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public final class IdentityDtos {

    private IdentityDtos() {}

    public record Register(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName
    ) {}

    public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {}

    public record EmailRequest(@NotBlank @Email String email) {}

    public record Reset(@NotBlank String token, @NotBlank @Size(min = 12, max = 72) String password) {}

    public record Verify(@NotBlank String token) {}

    public record Password(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 12, max = 72) String newPassword
    ) {}

    public record Profile(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 30) String phone
    ) {}

    public record AdminAccount(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank String role
    ) {}

    public record Access(boolean active, @NotEmpty Set<String> roles) {}

    public record UserView(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean active,
        boolean emailVerified,
        Instant lastLoginAt,
        Set<String> roles
    ) {
        public static UserView of(UserAccount u) {
            return new UserView(
                u.id,
                u.email,
                u.firstName,
                u.lastName,
                u.phone,
                u.active,
                u.emailVerified,
                u.lastLoginAt,
                u.roles
                    .stream()
                    .map(r -> r.name)
                    .collect(Collectors.toSet())
            );
        }
    }

    public record Session(String accessToken, long expiresIn, UserView user) {}
}
