package com.schoolmanagement.identity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_tokens")
public class AccountToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    public UserAccount user;

    @Column(length = 64, unique = true, nullable = false, columnDefinition = "char(64)")
    public String tokenHash;

    public String purpose;
    public Instant expiresAt;
    public Instant consumedAt;
}
