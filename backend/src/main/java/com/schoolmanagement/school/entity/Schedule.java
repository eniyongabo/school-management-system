package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_subject_id")
    public ClassSubject offering;

    public int weekday;
    public LocalTime startTime;
    public LocalTime endTime;
    public String room;
}
