package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String title;
    public String description;
    public String category;
    public Instant startAt;
    public Instant endAt;
    public boolean allDay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    public UserAccount author;
}
