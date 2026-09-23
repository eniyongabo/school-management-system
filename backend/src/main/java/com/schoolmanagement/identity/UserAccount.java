package com.schoolmanagement.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 254)
    public String email;

    @Column(nullable = false, length = 100)
    public String passwordHash;

    public String firstName;
    public String lastName;
    public String phone;
    public boolean active;
    public boolean emailVerified;
    public int tokenVersion;
    public Instant lastLoginAt;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    @Version
    public long version;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    public Set<Role> roles = new HashSet<>();

    public boolean has(String role) {
        return roles.stream().anyMatch(r -> r.name.equals(role));
    }
}
