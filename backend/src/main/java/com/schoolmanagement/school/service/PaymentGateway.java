package com.schoolmanagement.school.service;

import java.math.BigDecimal;

public interface PaymentGateway {
    String charge(long invoiceId, BigDecimal amount, String currency, String idempotencyKey);
}
