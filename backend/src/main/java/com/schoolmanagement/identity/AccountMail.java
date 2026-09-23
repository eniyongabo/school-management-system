package com.schoolmanagement.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class AccountMail {

    private final JavaMailSender sender;
    private final String origin;
    private final String from;

    public AccountMail(
        JavaMailSender sender,
        @Value("${app.cors.allowed-origin}") String origin,
        @Value("${app.mail.from}") String from
    ) {
        this.sender = sender;
        this.origin = origin;
        this.from = from;
    }

    public void send(String email, String token, String purpose) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(
            purpose.equals("PASSWORD_RESET")
                ? "Reset your school account password"
                : "Verify your school account"
        );
        message.setText(
            "Open this link within 30 minutes: " +
                origin +
                (purpose.equals("PASSWORD_RESET") ? "/reset-password" : "/verify-email") +
                "#token=" +
                token
        );
        sender.send(message);
    }
}
