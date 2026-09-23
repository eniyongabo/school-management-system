package com.schoolmanagement.school.entity;

import com.schoolmanagement.identity.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    public Invoice invoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payer_id")
    public UserAccount payer;

    public BigDecimal amount;
    public String currency;
    public String idempotencyKey;
    public String providerReference;
    public String status;
    public Instant paidAt;
}
