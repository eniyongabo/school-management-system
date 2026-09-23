package com.schoolmanagement.identity;

import static com.schoolmanagement.identity.IdentityDtos.*;
import static org.springframework.http.HttpStatus.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IdentityService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private final UserRepository users;
    private final RoleRepository roles;
    private final TokenRepository tokens;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final AccountMail mail;
    private final CurrentUser current;

    public IdentityService(
        UserRepository u,
        RoleRepository r,
        TokenRepository t,
        PasswordEncoder p,
        JwtEncoder e,
        AccountMail m,
        CurrentUser c
    ) {
        users = u;
        roles = r;
        tokens = t;
        passwords = p;
        encoder = e;
        mail = m;
        current = c;
    }

    public UserAccount create(
        String email,
        String password,
        String first,
        String last,
        String role,
        boolean active
    ) {
        email = email.strip().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) throw new ResponseStatusException(
            CONFLICT,
            "Email already registered"
        );
        var u = new UserAccount();
        u.email = email;
        u.passwordHash = hashPassword(password);
        u.firstName = first.strip();
        u.lastName = last.strip();
        u.active = true;
        u.emailVerified = active;
        u.roles.add(
            roles.findByName(role).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown role"))
        );
        users.saveAndFlush(u);
        if (role.equals("PARENT")) {
            var parent = new com.schoolmanagement.school.entity.Parent();
            parent.user = u;
            em.persist(parent);
        }
        return u;
    }

    public UserView register(Register r) {
        var u = create(r.email(), r.password(), r.firstName(), r.lastName(), "PARENT", false);
        issue(u, "EMAIL_VERIFICATION");
        return UserView.of(u);
    }

    public Session login(Login r) {
        var u = users.findByEmail(r.email().strip().toLowerCase(Locale.ROOT)).orElse(null);
        // Always run BCrypt, including for nonexistent accounts.
        String hash =
            u == null ? "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW" : u.passwordHash;
        boolean valid = passwords.matches(r.password(), hash);
        if (!valid || u == null || !u.active || !u.emailVerified) throw new ResponseStatusException(
            UNAUTHORIZED,
            "Invalid credentials or inactive account"
        );
        u.lastLoginAt = Instant.now();
        var claims = JwtClaimsSet.builder()
            .issuer("school-management")
            .audience(List.of("school-api"))
            .subject(u.id.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(1800))
            .claim("version", u.tokenVersion)
            .build();
        String token = encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
        return new Session(token, 1800, UserView.of(u));
    }

    public void logout() {
        current.get().tokenVersion++;
    }

    public UserView profile(Profile p) {
        var u = current.get();
        u.firstName = p.firstName();
        u.lastName = p.lastName();
        u.phone = p.phone();
        u.updatedAt = Instant.now();
        return UserView.of(u);
    }

    public void password(Password p) {
        var u = current.get();
        if (!passwords.matches(p.currentPassword(), u.passwordHash)) throw new ResponseStatusException(
            BAD_REQUEST,
            "Current password is incorrect"
        );
        u.passwordHash = hashPassword(p.newPassword());
        u.tokenVersion++;
    }

    public void forgot(String email) {
        users
            .findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .filter(u -> u.active)
            .ifPresent(u -> issue(u, "PASSWORD_RESET"));
    }

    public void resend(String email) {
        users
            .findByEmail(email.strip().toLowerCase(Locale.ROOT))
            .filter(u -> !u.emailVerified)
            .ifPresent(u -> issue(u, "EMAIL_VERIFICATION"));
    }

    public void reset(Reset r) {
        var t = consume(r.token(), "PASSWORD_RESET");
        t.user.passwordHash = hashPassword(r.password());
        t.user.tokenVersion++;
    }

    public void verify(String token) {
        var t = consume(token, "EMAIL_VERIFICATION");
        t.user.emailVerified = true;
    }

    public UserView access(long id, Access input) {
        var actor = current.require("ADMINISTRATOR");
        var u = users.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (actor.id.equals(id)) throw new ResponseStatusException(
            BAD_REQUEST,
            "Use another administrator to change your access"
        );
        var assigned = new HashSet<Role>();
        for (String r : input.roles())
            assigned.add(
                roles
                    .findByName(r)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown role"))
            );
        u.roles = assigned;
        u.active = input.active();
        u.tokenVersion++;
        return UserView.of(u);
    }

    private String hashPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(
            BAD_REQUEST,
            "Password exceeds 72 UTF-8 bytes"
        );
        return passwords.encode(password);
    }

    private void issue(UserAccount u, String purpose) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var t = new AccountToken();
        t.user = u;
        t.tokenHash = digest(raw);
        t.purpose = purpose;
        t.expiresAt = Instant.now().plusSeconds(1800);
        tokens.save(t);
        mail.send(u.email, raw, purpose);
    }

    private AccountToken consume(String raw, String purpose) {
        var t = tokens
            .findByTokenHash(digest(raw))
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid or expired token"));
        if (
            !t.purpose.equals(purpose) || t.consumedAt != null || t.expiresAt.isBefore(Instant.now())
        ) throw new ResponseStatusException(BAD_REQUEST, "Invalid or expired token");
        t.consumedAt = Instant.now();
        return t;
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
