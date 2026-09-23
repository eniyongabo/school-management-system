package com.schoolmanagement.config;

import com.schoolmanagement.identity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class AdministratorBootstrap {

    @Bean
    CommandLineRunner bootstrap(
        IdentityService identity,
        UserRepository users,
        @Value("${app.bootstrap.email:}") String email,
        @Value("${app.bootstrap.password:}") String password
    ) {
        return args -> {
            if (!email.isBlank() && users.count() == 0) {
                if (password.length() < 12) throw new IllegalArgumentException(
                    "Bootstrap password must have at least 12 characters"
                );
                identity.create(email, password, "School", "Administrator", "ADMINISTRATOR", true);
            }
        };
    }
}
