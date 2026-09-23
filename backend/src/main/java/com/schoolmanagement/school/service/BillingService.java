package com.schoolmanagement.school.service;

import static com.schoolmanagement.school.dto.SchoolDtos.*;
import static org.springframework.http.HttpStatus.*;

import com.schoolmanagement.identity.*;
import com.schoolmanagement.school.entity.*;
import com.schoolmanagement.school.repository.SchoolRepository;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BillingService {

    private final SchoolRepository db;
    private final CurrentUser current;
    private final SchoolAccess access;
    private final PaymentGateway gateway;
    private final LearningService audit;

    public BillingService(
        SchoolRepository d,
        CurrentUser c,
        SchoolAccess a,
        PaymentGateway g,
        LearningService l
    ) {
        db = d;
        current = c;
        access = a;
        gateway = g;
        audit = l;
    }

    public View fee(FeeInput p) {
        return fee(null, p);
    }

    public View fee(Long id, FeeInput p) {
        current.require("ADMINISTRATOR", "ACCOUNTANT");
        var f = id == null ? new FeeStructure() : db.get(FeeStructure.class, id);
        f.academicYear = db.get(AcademicYear.class, p.academicYearId());
        f.schoolClass = p.classId() == null ? null : db.get(SchoolClass.class, p.classId());
        if (f.schoolClass != null && !f.schoolClass.academicYear.id.equals(f.academicYear.id)) throw bad(
            "Class is outside this academic year"
        );
        f.name = p.name();
        f.amount = p.amount();
        f.currency = p.currency();
        if (id == null) db.save(f);
        return feeView(f);
    }

    public List<View> fees() {
        current.require("ADMINISTRATOR", "ACCOUNTANT");
        return db
            .query(FeeStructure.class, "select f from FeeStructure f order by f.id desc")
            .stream()
            .map(this::feeView)
            .toList();
    }

    private View feeView(FeeStructure f) {
        return view(
            "id",
            f.id,
            "name",
            f.name,
            "academicYearId",
            f.academicYear.id,
            "classId",
            f.schoolClass == null ? null : f.schoolClass.id,
            "amount",
            f.amount,
            "currency",
            f.currency
        );
    }

    public View invoice(InvoiceInput p) {
        current.require("ADMINISTRATOR", "ACCOUNTANT");
        var s = db.get(Student.class, p.studentId());
        var f = db.get(FeeStructure.class, p.feeStructureId());
        if (f.schoolClass != null) access.enrolled(s, f.schoolClass);
        var i = new Invoice();
        i.student = s;
        i.invoiceNumber = "INV-" + UUID.randomUUID();
        i.description = f.name;
        i.amount = f.amount;
        i.currency = f.currency;
        i.dueDate = p.dueDate();
        i.issuedAt = Instant.now();
        db.save(i);
        audit.audit("CREATE_INVOICE", "Invoice", i.id);
        return invoiceView(i);
    }

    public List<View> invoices(Long studentId) {
        current.require("ADMINISTRATOR", "ACCOUNTANT", "PARENT", "STUDENT");
        if (studentId != null) access.student(studentId, false);
        return db
            .query(Invoice.class, "select i from Invoice i order by i.issuedAt desc")
            .stream()
            .filter(i -> studentId == null || i.student.id.equals(studentId))
            .filter(i -> access.studentVisible(i.student))
            .map(this::invoiceView)
            .toList();
    }

    private BigDecimal paid(Invoice i) {
        return db
            .query(
                Payment.class,
                "select p from Payment p where p.invoice.id=?1 and p.status='SUCCEEDED'",
                i.id
            )
            .stream()
            .map(p -> p.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private View invoiceView(Invoice i) {
        var paid = paid(i);
        return view(
            "id",
            i.id,
            "studentId",
            i.student.id,
            "student",
            i.student.firstName + " " + i.student.lastName,
            "invoiceNumber",
            i.invoiceNumber,
            "description",
            i.description,
            "amount",
            i.amount,
            "currency",
            i.currency,
            "paid",
            paid,
            "balance",
            i.amount.subtract(paid),
            "status",
            paid.compareTo(i.amount) == 0 ? "PAID" : paid.signum() > 0 ? "PARTIAL" : "UNPAID",
            "dueDate",
            i.dueDate
        );
    }

    public View pay(PaymentInput input, String key) {
        var u = current.require("PARENT", "ACCOUNTANT", "ADMINISTRATOR");
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,100}")) throw bad(
            "A 16–100 character Idempotency-Key is required"
        );
        var i = db.lock(Invoice.class, input.invoiceId());
        access.student(i.student.id, false);
        var prior = db.query(Payment.class, "select p from Payment p where p.idempotencyKey=?1", key);
        if (!prior.isEmpty()) {
            var p = prior.get(0);
            if (
                !p.payer.id.equals(u.id) ||
                !p.invoice.id.equals(i.id) ||
                p.amount.compareTo(input.amount()) != 0
            ) throw new ResponseStatusException(CONFLICT, "Idempotency key already used for another payment");
            return paymentView(p);
        }
        if (input.amount().compareTo(i.amount.subtract(paid(i))) > 0) throw bad(
            "Payment exceeds outstanding balance"
        );
        var p = new Payment();
        p.invoice = i;
        p.payer = u;
        p.amount = input.amount();
        p.currency = i.currency;
        p.idempotencyKey = key;
        p.providerReference = gateway.charge(i.id, p.amount, p.currency, key);
        p.status = "SUCCEEDED";
        p.paidAt = Instant.now();
        db.save(p);
        var receipt = new Receipt();
        receipt.payment = p;
        receipt.receiptNumber = "RCP-" + UUID.randomUUID();
        receipt.issuedAt = Instant.now();
        db.save(receipt);
        audit.audit("MOCK_PAYMENT", "Payment", p.id);
        return paymentView(p);
    }

    public List<View> payments() {
        current.require("ADMINISTRATOR", "ACCOUNTANT", "PARENT", "STUDENT");
        return db
            .query(Payment.class, "select p from Payment p order by p.paidAt desc")
            .stream()
            .filter(p -> access.studentVisible(p.invoice.student))
            .map(this::paymentView)
            .toList();
    }

    private View paymentView(Payment p) {
        var receipts = db.query(Receipt.class, "select r from Receipt r where r.payment.id=?1", p.id);
        return view(
            "id",
            p.id,
            "invoiceId",
            p.invoice.id,
            "student",
            p.invoice.student.firstName + " " + p.invoice.student.lastName,
            "amount",
            p.amount,
            "currency",
            p.currency,
            "status",
            p.status,
            "provider",
            "MOCK — no real money processed",
            "paidAt",
            p.paidAt,
            "receiptId",
            receipts.isEmpty() ? null : receipts.get(0).id
        );
    }

    public String receipt(long id) {
        current.require("ADMINISTRATOR", "ACCOUNTANT", "PARENT", "STUDENT");
        var r = db.get(Receipt.class, id);
        access.student(r.payment.invoice.student.id, false);
        return (
            "SCHOOL MANAGEMENT — MOCK PAYMENT RECEIPT\nNo real money was processed.\n\nReceipt: " +
            r.receiptNumber +
            "\nInvoice: " +
            r.payment.invoice.invoiceNumber +
            "\nStudent: " +
            r.payment.invoice.student.firstName +
            " " +
            r.payment.invoice.student.lastName +
            "\nAmount: " +
            r.payment.currency +
            " " +
            r.payment.amount +
            "\nIssued: " +
            r.issuedAt +
            "\nReference: " +
            r.payment.providerReference +
            "\n"
        );
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
