package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    public Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_subject_id")
    public ClassSubject offering;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term_id")
    public Term term;

    public String type;
    public String title;
    public BigDecimal score;
    public BigDecimal maximum;
    public BigDecimal weight;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    public UserAccount author;

    public Instant updatedAt;

    @Version
    public long version;
}
