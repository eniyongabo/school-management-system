package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public UserAccount user;

    public String studentNumber;
    public String firstName;
    public String lastName;
    public LocalDate dateOfBirth;
    public String gender;
    public String address;
    public String emergencyContactName;
    public String emergencyContactPhone;
    public LocalDate enrollmentDate;
    public String status;

    @Version
    public long version;
}
