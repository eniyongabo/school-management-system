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
@Transactional(readOnly = true)
public class ReportService {

    private final PeopleService people;
    private final AcademicService academics;
    private final LearningService learning;
    private final BillingService billing;
    private final CommunicationService communications;
    private final CurrentUser current;
    private final SchoolRepository db;

    public ReportService(
        PeopleService p,
        AcademicService a,
        LearningService l,
        BillingService b,
        CommunicationService c,
        CurrentUser u,
        SchoolRepository d
    ) {
        people = p;
        academics = a;
        learning = l;
        billing = b;
        communications = c;
        current = u;
        db = d;
    }

    public View dashboard() {
        var u = current.get();
        boolean academic = !u.has("ACCOUNTANT") || u.has("ADMINISTRATOR");
        boolean financial = !u.has("TEACHER") || u.has("ADMINISTRATOR");
        var invoices = financial ? billing.invoices(null) : List.<View>of();
        var totals = new LinkedHashMap<String, BigDecimal>();
        for (var i : invoices)
            totals.merge(
                (String) i.fields().get("currency"),
                (BigDecimal) i.fields().get("balance"),
                BigDecimal::add
            );
        var collected = new LinkedHashMap<String, BigDecimal>();
        if (financial) for (var payment : billing.payments())
            collected.merge(
                (String) payment.fields().get("currency"),
                (BigDecimal) payment.fields().get("amount"),
                BigDecimal::add
            );
        return view(
            "pendingGrades",
            u.has("ADMINISTRATOR") || u.has("TEACHER") ? learning.pendingGradeCount() : null,
            "children",
            u.has("PARENT") ? people.students() : List.of(),
            "upcomingAssignments",
            academic
                ? learning
                      .assignments()
                      .stream()
                      .filter(a -> ((Instant) a.fields().get("dueAt")).isAfter(Instant.now()))
                      .limit(5)
                      .toList()
                : List.of(),
            "enrollmentByClass",
            u.has("ADMINISTRATOR") ? academics.classes() : List.of(),
            "collectedByCurrency",
            collected,
            "students",
            people.students().size(),
            "classes",
            academics.classes().size(),
            "teachers",
            people.teachers().size(),
            "parents",
            u.has("ADMINISTRATOR") ? db.count("select count(p) from Parent p") : null,
            "assignments",
            academic ? learning.assignments().size() : null,
            "attendance",
            academic
                ? learning
                      .attendance((Long) null)
                      .stream()
                      .collect(
                          java.util.stream.Collectors.groupingBy(
                              v -> v.fields().get("status"),
                              java.util.stream.Collectors.counting()
                          )
                      )
                : Map.of(),
            "outstandingByCurrency",
            totals,
            "recentGrades",
            academic ? learning.grades(null).stream().limit(5).toList() : List.of(),
            "announcements",
            communications.announcements().stream().limit(4).toList(),
            "upcomingEvents",
            communications
                .events()
                .stream()
                .filter(e -> ((Instant) e.fields().get("endAt")).isAfter(Instant.now()))
                .limit(5)
                .toList(),
            "unreadNotifications",
            communications
                .notifications()
                .stream()
                .filter(n -> n.fields().get("readAt") == null)
                .count()
        );
    }

    public List<View> report(String type) {
        current.require("ADMINISTRATOR", "ACCOUNTANT");
        if (
            current.get().has("ACCOUNTANT") &&
            !current.get().has("ADMINISTRATOR") &&
            !Set.of("fees", "payments", "outstanding").contains(type)
        ) throw new ResponseStatusException(FORBIDDEN);
        return switch (type) {
            case "enrollment" -> academics.enrollments();
            case "attendance" -> learning.attendance((Long) null);
            case "grades" -> learning.grades(null);
            case "fees" -> billing.invoices(null);
            case "payments" -> billing.payments();
            case "outstanding" -> billing
                .invoices(null)
                .stream()
                .filter(i -> ((BigDecimal) i.fields().get("balance")).signum() > 0)
                .toList();
            case "teachers" -> people.teachers();
            case "classes" -> classPerformance();
            case "audit" -> audit();
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unknown report type");
        };
    }

    private List<View> classPerformance() {
        return academics
            .offerings()
            .stream()
            .map(o -> {
                long id = ((Number) o.fields().get("id")).longValue();
                var scores = learning
                    .grades(null)
                    .stream()
                    .filter(g -> ((Number) g.fields().get("offeringId")).longValue() == id)
                    .map(g -> (BigDecimal) g.fields().get("percentage"))
                    .toList();
                return view(
                    "class",
                    o.fields().get("className"),
                    "subject",
                    o.fields().get("subject"),
                    "gradedAssessments",
                    scores.size(),
                    "assessmentAverage",
                    scores.isEmpty()
                        ? null
                        : scores
                              .stream()
                              .reduce(BigDecimal.ZERO, BigDecimal::add)
                              .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP)
                );
            })
            .toList();
    }

    private List<View> audit() {
        return db
            .query(AuditLog.class, "select a from AuditLog a order by a.occurredAt desc")
            .stream()
            .map(a ->
                view(
                    "id",
                    a.id,
                    "actorId",
                    a.actor.id,
                    "action",
                    a.action,
                    "resourceType",
                    a.resourceType,
                    "resourceId",
                    a.resourceId,
                    "occurredAt",
                    a.occurredAt
                )
            )
            .toList();
    }

    public String reportCard(long id) {
        var student = people.student(id);
        var grades = learning.grades(id);
        var averages = learning.averages(id);
        var report = new StringBuilder("SCHOOL REPORT CARD\n");
        report
            .append("Student: ")
            .append(student.fields().get("firstName"))
            .append(' ')
            .append(student.fields().get("lastName"))
            .append("\nStudent number: ")
            .append(student.fields().get("studentNumber"))
            .append("\nGenerated: ")
            .append(Instant.now())
            .append("\n\nASSESSMENTS\n");
        for (var grade : grades) {
            var fields = grade.fields();
            report
                .append(fields.get("subject"))
                .append(" | ")
                .append(fields.get("term"))
                .append(" | ")
                .append(fields.get("title"))
                .append(": ")
                .append(fields.get("score"))
                .append('/')
                .append(fields.get("maximum"))
                .append(" (")
                .append(fields.get("letter"))
                .append(")\n");
        }
        report.append("\nSUBJECT AVERAGES\n");
        for (Object subject : (List<?>) averages.fields().get("subjects")) {
            var fields = ((View) subject).fields();
            report
                .append(fields.get("subject"))
                .append(" | ")
                .append(fields.get("term"))
                .append(": ")
                .append(fields.get("average"))
                .append("% (")
                .append(fields.get("letter"))
                .append(")\n");
        }
        report
            .append("\nOverall average: ")
            .append(
                averages.fields().get("overallAverage") == null
                    ? "Not yet graded"
                    : averages.fields().get("overallAverage") + "%"
            )
            .append("\n");
        return report.toString();
    }

    public String csv(List<View> rows) {
        if (rows.isEmpty()) return "No records\r\n";
        var keys = rows.get(0).fields().keySet();
        var out = new StringBuilder();
        out.append(String.join(",", keys)).append("\r\n");
        for (var row : rows) {
            out.append(
                keys
                    .stream()
                    .map(k -> cell(row.fields().get(k)))
                    .collect(java.util.stream.Collectors.joining(","))
            ).append("\r\n");
        }
        return out.toString();
    }

    private String cell(Object value) {
        String s = value == null ? "" : value.toString();
        if (!s.stripLeading().isEmpty() && "=+-@\t\r\n".indexOf(s.stripLeading().charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
