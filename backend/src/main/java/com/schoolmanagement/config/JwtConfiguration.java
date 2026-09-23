package com.schoolmanagement.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.schoolmanagement.identity.UserRepository;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class JwtConfiguration {

    @Bean
    SecretKeySpec jwtKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) throw new IllegalArgumentException(
            "JWT_SECRET must be base64 of at least 32 random bytes"
        );
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKeySpec key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKeySpec key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience = jwt ->
            jwt.getAudience().contains("school-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        decoder.setJwtValidator(
            new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("school-management"),
                audience
            )
        );
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> accountAuthentication(UserRepository users) {
        return new Converter<Jwt, AbstractAuthenticationToken>() {
            public AbstractAuthenticationToken convert(Jwt jwt) {
                try {
                    var u = users.findById(Long.parseLong(jwt.getSubject())).orElseThrow();
                    Number version = jwt.getClaim("version");
                    if (
                        !u.active ||
                        !u.emailVerified ||
                        version == null ||
                        version.intValue() != u.tokenVersion
                    ) throw new IllegalArgumentException();
                    return new JwtAuthenticationToken(
                        jwt,
                        u.roles
                            .stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name))
                            .toList(),
                        u.id.toString()
                    );
                } catch (RuntimeException e) {
                    throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_token"),
                        "Account or token is no longer valid"
                    );
                }
            }
        };
    }
}
