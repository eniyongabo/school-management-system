package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id")
    public UserAccount sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id")
    public UserAccount recipient;

    public String body;
    public Instant sentAt;
    public Instant readAt;
}
