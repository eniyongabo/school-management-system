package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    public UserAccount actor;

    public String action;
    public String resourceType;
    public Long resourceId;
    public Instant occurredAt;
}
