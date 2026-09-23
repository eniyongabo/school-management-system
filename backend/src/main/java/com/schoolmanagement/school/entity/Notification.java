package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id")
    public UserAccount recipient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "announcement_id")
    public Announcement announcement;

    public String title;
    public Instant createdAt;
    public Instant readAt;
}
