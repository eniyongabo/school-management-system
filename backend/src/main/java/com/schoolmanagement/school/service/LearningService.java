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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class LearningService {

    private final SchoolRepository db;
    private final CurrentUser current;
    private final SchoolAccess access;

    public LearningService(SchoolRepository d, CurrentUser c, SchoolAccess a) {
        db = d;
        current = c;
        access = a;
    }

    public View assignment(AssignmentInput p) {
        return assignment(null, p);
    }

    public View assignment(Long id, AssignmentInput p) {
        var a = id == null ? new Assignment() : db.get(Assignment.class, id);
        if (id != null) {
            access.offering(a.offering.id, true);
            if (!a.offering.id.equals(p.offeringId())) throw bad(
                "Cannot move an existing assignment to another offering"
            );
        }
        a.offering = access.offering(p.offeringId(), true);
        a.title = p.title();
        a.description = p.description();
        a.dueAt = p.dueAt();
        if (id == null) db.save(a);
        return assignmentView(a);
    }

    public View assignment(long id) {
        var a = db.get(Assignment.class, id);
        access.offering(a.offering.id, false);
        return assignmentView(a);
    }

    public List<View> assignments() {
        current.require("ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT");
        return db
            .query(Assignment.class, "select a from Assignment a order by a.dueAt")
            .stream()
            .filter(a -> access.classVisible(a.offering.schoolClass))
            .map(this::assignmentView)
            .toList();
    }

    private View assignmentView(Assignment a) {
        return view(
            "id",
            a.id,
            "offeringId",
            a.offering.id,
            "className",
            a.offering.schoolClass.name,
            "subject",
            a.offering.subject.name,
            "title",
            a.title,
            "description",
            a.description,
            "dueAt",
            a.dueAt,
            "submissionCount",
            current.get().has("PARENT") || current.get().has("STUDENT")
                ? null
                : db.count("select count(s) from Submission s where s.assignment.id=?1", a.id)
        );
    }

    public View submit(long id, SubmissionInput p) {
        var u = current.require("STUDENT");
        var a = db.get(Assignment.class, id);
        var s = access.student(p.studentId(), true);
        if (s.user == null || !s.user.id.equals(u.id)) throw SchoolAccess.hidden();
        access.enrolled(s, a.offering.schoolClass);
        if (Instant.now().isAfter(a.dueAt)) throw bad("The submission deadline has passed");
        var existing = db.query(
            Submission.class,
            "select s from Submission s where s.assignment.id=?1 and s.student.id=?2",
            id,
            s.id
        );
        var sub = existing.isEmpty() ? new Submission() : existing.get(0);
        sub.assignment = a;
        sub.student = s;
        sub.body = p.body();
        sub.submittedAt = Instant.now();
        if (sub.id == null) db.save(sub);
        return submissionView(sub);
    }

    public List<View> submissions(long id) {
        var a = db.get(Assignment.class, id);
        access.offering(a.offering.id, false);
        var u = current.get();
        return db
            .query(Submission.class, "select s from Submission s where s.assignment.id=?1", id)
            .stream()
            .filter(s -> access.studentVisible(s.student))
            .filter(
                s -> !u.has("TEACHER") || u.has("ADMINISTRATOR") || a.offering.teacher.user.id.equals(u.id)
            )
            .map(this::submissionView)
            .toList();
    }

    private View submissionView(Submission s) {
        return view(
            "id",
            s.id,
            "studentId",
            s.student.id,
            "student",
            s.student.firstName + " " + s.student.lastName,
            "body",
            s.body,
            "submittedAt",
            s.submittedAt
        );
    }

    public View grade(Long id, GradeInput p) {
        var o = access.offering(p.offeringId(), true);
        var s = access.student(p.studentId(), true);
        access.enrolled(s, o.schoolClass);
        var term = db.get(Term.class, p.termId());
        if (!term.academicYear.id.equals(o.schoolClass.academicYear.id)) throw bad(
            "Term and class must belong to the same academic year"
        );
        if (p.score().compareTo(p.maximum()) > 0) throw bad("Score exceeds maximum");
        var g = id == null ? new Grade() : db.get(Grade.class, id);
        if (id != null) {
            access.offering(g.offering.id, true);
            if (!g.student.id.equals(s.id) || !g.offering.id.equals(o.id)) throw bad(
                "Cannot move a grade to another student or offering"
            );
        }
        g.student = s;
        g.offering = o;
        g.term = term;
        g.type = p.type();
        g.title = p.title();
        g.score = p.score();
        g.maximum = p.maximum();
        g.weight = p.weight();
        g.author = current.get();
        g.updatedAt = Instant.now();
        if (id == null) db.save(g);
        audit(id == null ? "CREATE_GRADE" : "UPDATE_GRADE", "Grade", g.id);
        return gradeView(g);
    }

    public long pendingGradeCount() {
        var user = current.require("ADMINISTRATOR", "TEACHER");
        long pending = 0;
        for (var offering : db.query(ClassSubject.class, "select o from ClassSubject o")) {
            if (!user.has("ADMINISTRATOR") && !offering.teacher.user.id.equals(user.id)) continue;
            pending += db.count(
                "select count(e) from Enrollment e where e.schoolClass.id=?1 and e.withdrawnOn is null and not exists (select g.id from Grade g where g.student.id=e.student.id and g.offering.id=?2)",
                offering.schoolClass.id,
                offering.id
            );
        }
        return pending;
    }

    public List<View> grades(Long studentId) {
        if (studentId != null) access.student(studentId, true);
        current.require("ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT");
        return visibleGrades(studentId).stream().map(this::gradeView).toList();
    }

    private List<Grade> visibleGrades(Long id) {
        var u = current.get();
        return db
            .query(Grade.class, "select g from Grade g order by g.updatedAt desc")
            .stream()
            .filter(g -> id == null || g.student.id.equals(id))
            .filter(g -> access.studentVisible(g.student))
            .filter(
                g -> !u.has("TEACHER") || u.has("ADMINISTRATOR") || g.offering.teacher.user.id.equals(u.id)
            )
            .toList();
    }

    private View gradeView(Grade g) {
        return view(
            "id",
            g.id,
            "studentId",
            g.student.id,
            "student",
            g.student.firstName + " " + g.student.lastName,
            "offeringId",
            g.offering.id,
            "subject",
            g.offering.subject.name,
            "termId",
            g.term.id,
            "term",
            g.term.name,
            "type",
            g.type,
            "title",
            g.title,
            "score",
            g.score,
            "maximum",
            g.maximum,
            "weight",
            g.weight,
            "percentage",
            percent(g),
            "letter",
            letter(percent(g)),
            "updatedAt",
            g.updatedAt
        );
    }

    public View averages(long studentId) {
        access.student(studentId, true);
        var all = visibleGrades(studentId);
        var groups = new LinkedHashMap<String, List<Grade>>();
        for (var g : all)
            groups.computeIfAbsent(g.offering.id + ":" + g.term.id, k -> new ArrayList<>()).add(g);
        var rows = new ArrayList<View>();
        BigDecimal total = BigDecimal.ZERO;
        for (var list : groups.values()) {
            BigDecimal points = BigDecimal.ZERO,
                weights = BigDecimal.ZERO;
            for (var g : list) {
                points = points.add(percent(g).multiply(g.weight));
                weights = weights.add(g.weight);
            }
            var avg = points.divide(weights, 2, RoundingMode.HALF_UP);
            total = total.add(avg);
            var first = list.get(0);
            rows.add(
                view(
                    "subject",
                    first.offering.subject.name,
                    "term",
                    first.term.name,
                    "average",
                    avg,
                    "letter",
                    letter(avg)
                )
            );
        }
        return view(
            "studentId",
            studentId,
            "subjects",
            rows,
            "overallAverage",
            rows.isEmpty() ? null : total.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal percent(Grade g) {
        return g.score.multiply(BigDecimal.valueOf(100)).divide(g.maximum, 4, RoundingMode.HALF_UP);
    }

    public static String letter(BigDecimal score) {
        return score.compareTo(BigDecimal.valueOf(90)) >= 0
            ? "A"
            : score.compareTo(BigDecimal.valueOf(80)) >= 0
              ? "B"
              : score.compareTo(BigDecimal.valueOf(70)) >= 0
                ? "C"
                : score.compareTo(BigDecimal.valueOf(60)) >= 0
                  ? "D"
                  : "F";
    }

    public View attendance(AttendanceInput p) {
        access.classWrite(p.classId());
        var s = access.student(p.studentId(), true);
        var c = db.get(SchoolClass.class, p.classId());
        access.enrolled(s, c);
        if (
            p.date().isBefore(c.academicYear.startDate) || p.date().isAfter(c.academicYear.endDate)
        ) throw bad("Attendance date is outside the academic year");
        var found = db.query(
            Attendance.class,
            "select a from Attendance a where a.student.id=?1 and a.schoolClass.id=?2 and a.attendanceDate=?3",
            s.id,
            c.id,
            p.date()
        );
        var a = found.isEmpty() ? new Attendance() : found.get(0);
        a.student = s;
        a.schoolClass = c;
        a.attendanceDate = p.date();
        a.status = p.status();
        a.author = current.get();
        if (a.id == null) db.save(a);
        audit("RECORD_ATTENDANCE", "Attendance", a.id);
        return attendanceView(a);
    }

    public List<View> attendance(Long studentId) {
        if (studentId != null) access.student(studentId, true);
        current.require("ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT");
        return db
            .query(Attendance.class, "select a from Attendance a order by a.attendanceDate desc")
            .stream()
            .filter(a -> studentId == null || a.student.id.equals(studentId))
            .filter(a -> access.studentVisible(a.student) && access.classVisible(a.schoolClass))
            .map(this::attendanceView)
            .toList();
    }

    private View attendanceView(Attendance a) {
        return view(
            "id",
            a.id,
            "studentId",
            a.student.id,
            "student",
            a.student.firstName + " " + a.student.lastName,
            "classId",
            a.schoolClass.id,
            "className",
            a.schoolClass.name,
            "date",
            a.attendanceDate,
            "status",
            a.status
        );
    }

    public View upload(long id, MultipartFile file) {
        var a = db.get(Assignment.class, id);
        access.offering(a.offering.id, true);
        String name =
            file.getOriginalFilename() == null
                ? "material"
                : file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._ -]", "_");
        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024 || name.length() > 255) throw bad(
            "File must be between 1 byte and 10 MB"
        );
        String type = file.getContentType();
        if (
            !Set.of("application/pdf", "text/plain", "image/png", "image/jpeg").contains(
                type == null ? "" : type
            )
        ) throw bad("Only PDF, text, PNG and JPEG materials are accepted");
        try {
            var d = new Document();
            d.assignment = a;
            d.uploader = current.get();
            d.filename = name;
            d.mediaType = type;
            d.content = file.getBytes();
            d.uploadedAt = Instant.now();
            db.save(d);
            return documentView(d);
        } catch (java.io.IOException e) {
            throw bad("Could not read upload");
        }
    }

    public List<View> documents(long id) {
        var a = db.get(Assignment.class, id);
        access.offering(a.offering.id, false);
        return db
            .query(Document.class, "select d from Document d where d.assignment.id=?1", id)
            .stream()
            .map(this::documentView)
            .toList();
    }

    private View documentView(Document d) {
        return view("id", d.id, "filename", d.filename, "mediaType", d.mediaType, "uploadedAt", d.uploadedAt);
    }

    public record Download(String filename, byte[] bytes) {}

    public Download download(long id) {
        var d = db.get(Document.class, id);
        access.offering(d.assignment.offering.id, false);
        return new Download(d.filename, d.content.clone());
    }

    public void audit(String action, String type, long id) {
        var a = new AuditLog();
        a.actor = current.get();
        a.action = action;
        a.resourceType = type;
        a.resourceId = id;
        a.occurredAt = Instant.now();
        db.save(a);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
