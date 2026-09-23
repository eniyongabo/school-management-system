package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    public UserAccount author;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id")
    public SchoolClass schoolClass;

    public String title;
    public String body;
    public String priority;
    public Instant publishedAt;
}
