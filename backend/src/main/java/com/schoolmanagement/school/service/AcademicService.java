package com.schoolmanagement.school.service;

import static com.schoolmanagement.school.dto.SchoolDtos.*;
import static org.springframework.http.HttpStatus.*;

import com.schoolmanagement.identity.*;
import com.schoolmanagement.school.entity.*;
import com.schoolmanagement.school.repository.SchoolRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AcademicService {

    private final SchoolRepository db;
    private final CurrentUser current;
    private final SchoolAccess access;

    public AcademicService(SchoolRepository d, CurrentUser c, SchoolAccess a) {
        db = d;
        current = c;
        access = a;
    }

    public View year(YearInput p) {
        return year(null, p);
    }

    public View year(Long id, YearInput p) {
        current.require("ADMINISTRATOR");
        dates(p.startDate(), p.endDate());
        var y = id == null ? new AcademicYear() : db.get(AcademicYear.class, id);
        if (id != null) {
            for (var term : db.query(Term.class, "select t from Term t where t.academicYear.id=?1", id)) {
                if (term.startDate.isBefore(p.startDate()) || term.endDate.isAfter(p.endDate())) throw bad(
                    "Year dates must include all existing terms"
                );
            }
            if (
                db.count(
                    "select count(a) from Attendance a where a.schoolClass.academicYear.id=?1 and (a.attendanceDate<?2 or a.attendanceDate>?3)",
                    id,
                    p.startDate(),
                    p.endDate()
                ) > 0
            ) throw bad("Year dates must include existing attendance records");
        }
        y.name = p.name();
        y.startDate = p.startDate();
        y.endDate = p.endDate();
        if (id == null) db.save(y);
        return yearView(y);
    }

    public List<View> years() {
        return db
            .query(AcademicYear.class, "select y from AcademicYear y order by y.startDate desc")
            .stream()
            .map(this::yearView)
            .toList();
    }

    private View yearView(AcademicYear y) {
        return view("id", y.id, "name", y.name, "startDate", y.startDate, "endDate", y.endDate);
    }

    public View term(TermInput p) {
        return term(null, p);
    }

    public View term(Long id, TermInput p) {
        current.require("ADMINISTRATOR");
        var y = db.get(AcademicYear.class, p.academicYearId());
        dates(p.startDate(), p.endDate());
        if (p.startDate().isBefore(y.startDate) || p.endDate().isAfter(y.endDate)) throw bad(
            "Term must lie inside academic year"
        );
        var t = id == null ? new Term() : db.get(Term.class, id);
        if (id != null && !t.academicYear.id.equals(y.id)) throw bad(
            "Existing terms cannot be moved to another academic year"
        );
        t.academicYear = y;
        t.name = p.name();
        t.startDate = p.startDate();
        t.endDate = p.endDate();
        if (id == null) db.save(t);
        return termView(t);
    }

    public List<View> terms() {
        return db
            .query(Term.class, "select t from Term t order by t.startDate")
            .stream()
            .map(this::termView)
            .toList();
    }

    private View termView(Term t) {
        return view(
            "id",
            t.id,
            "academicYearId",
            t.academicYear.id,
            "name",
            t.name,
            "startDate",
            t.startDate,
            "endDate",
            t.endDate
        );
    }

    public View schoolClass(ClassInput p) {
        return schoolClass(null, p);
    }

    public View schoolClass(Long id, ClassInput p) {
        current.require("ADMINISTRATOR");
        var c = id == null ? new SchoolClass() : db.get(SchoolClass.class, id);
        if (id != null && !c.academicYear.id.equals(p.academicYearId())) throw bad(
            "Existing classes cannot be moved to another academic year"
        );
        c.academicYear = db.get(AcademicYear.class, p.academicYearId());
        c.name = p.name();
        c.gradeLevel = p.gradeLevel();
        if (
            id != null &&
            db.count(
                "select count(e) from Enrollment e where e.schoolClass.id=?1 and e.withdrawnOn is null",
                id
            ) > p.capacity()
        ) throw bad("Capacity cannot be smaller than current enrollment");
        c.capacity = p.capacity();
        if (id == null) db.save(c);
        return classView(c);
    }

    public List<View> classes() {
        return db
            .query(SchoolClass.class, "select c from SchoolClass c order by c.name")
            .stream()
            .filter(access::classVisible)
            .map(this::classView)
            .toList();
    }

    private View classView(SchoolClass c) {
        return view(
            "id",
            c.id,
            "name",
            c.name,
            "gradeLevel",
            c.gradeLevel,
            "capacity",
            c.capacity,
            "academicYearId",
            c.academicYear.id,
            "academicYear",
            c.academicYear.name,
            "studentCount",
            db.count(
                "select count(e) from Enrollment e where e.schoolClass.id=?1 and e.withdrawnOn is null",
                c.id
            )
        );
    }

    public View subject(SubjectInput p) {
        return subject(null, p);
    }

    public View subject(Long id, SubjectInput p) {
        current.require("ADMINISTRATOR");
        var s = id == null ? new Subject() : db.get(Subject.class, id);
        s.name = p.name();
        s.code = p.code();
        if (id == null) db.save(s);
        return view("id", s.id, "name", s.name, "code", s.code);
    }

    public List<View> subjects() {
        return db
            .query(Subject.class, "select s from Subject s order by s.name")
            .stream()
            .map(s -> view("id", s.id, "name", s.name, "code", s.code))
            .toList();
    }

    public View offering(OfferingInput p) {
        return offering(null, p);
    }

    public View offering(Long id, OfferingInput p) {
        current.require("ADMINISTRATOR");
        var o = id == null ? new ClassSubject() : db.get(ClassSubject.class, id);
        if (
            id != null && (!o.schoolClass.id.equals(p.classId()) || !o.subject.id.equals(p.subjectId()))
        ) throw bad("Existing teaching assignments cannot be moved to another class or subject");
        o.schoolClass = db.get(SchoolClass.class, p.classId());
        o.subject = db.get(Subject.class, p.subjectId());
        o.teacher = db.get(Teacher.class, p.teacherId());
        if (id == null) db.save(o);
        return offeringView(o);
    }

    public List<View> offerings() {
        var u = current.get();
        return db
            .query(ClassSubject.class, "select o from ClassSubject o order by o.id")
            .stream()
            .filter(o -> access.classVisible(o.schoolClass))
            .filter(o -> !u.has("TEACHER") || u.has("ADMINISTRATOR") || o.teacher.user.id.equals(u.id))
            .map(this::offeringView)
            .toList();
    }

    private View offeringView(ClassSubject o) {
        return view(
            "id",
            o.id,
            "classId",
            o.schoolClass.id,
            "className",
            o.schoolClass.name,
            "subjectId",
            o.subject.id,
            "subject",
            o.subject.name,
            "teacherId",
            o.teacher.id,
            "teacher",
            o.teacher.user.firstName + " " + o.teacher.user.lastName
        );
    }

    public View enroll(EnrollmentInput p) {
        current.require("ADMINISTRATOR");
        var s = db.lock(Student.class, p.studentId());
        var c = db.lock(SchoolClass.class, p.classId());
        if (!s.status.equals("ACTIVE")) throw bad("Activate the student before enrollment");
        if (
            db.count(
                "select count(e) from Enrollment e where e.student.id=?1 and e.schoolClass.academicYear.id=?2 and e.withdrawnOn is null",
                s.id,
                c.academicYear.id
            ) > 0
        ) throw bad("Student already enrolled for this academic year");
        if (
            db.count(
                "select count(e) from Enrollment e where e.schoolClass.id=?1 and e.withdrawnOn is null",
                c.id
            ) >= c.capacity
        ) throw bad("Class is full");
        var e = new Enrollment();
        e.student = s;
        e.schoolClass = c;
        e.enrolledOn = LocalDate.now();
        db.save(e);
        return enrollmentView(e);
    }

    public List<View> enrollments() {
        return db
            .query(Enrollment.class, "select e from Enrollment e order by e.id")
            .stream()
            .filter(e -> access.studentVisible(e.student))
            .map(this::enrollmentView)
            .toList();
    }

    public void withdraw(long id) {
        current.require("ADMINISTRATOR");
        db.get(Enrollment.class, id).withdrawnOn = LocalDate.now();
    }

    private View enrollmentView(Enrollment e) {
        return view(
            "id",
            e.id,
            "studentId",
            e.student.id,
            "student",
            e.student.firstName + " " + e.student.lastName,
            "classId",
            e.schoolClass.id,
            "className",
            e.schoolClass.name,
            "enrolledOn",
            e.enrolledOn,
            "withdrawnOn",
            e.withdrawnOn
        );
    }

    public View schedule(ScheduleInput p) {
        return schedule(null, p);
    }

    public View schedule(Long id, ScheduleInput p) {
        current.require("ADMINISTRATOR");
        var o = access.offering(p.offeringId(), true);
        db.lock(AcademicYear.class, o.schoolClass.academicYear.id);
        if (!p.endTime().isAfter(p.startTime())) throw bad("End time must follow start time");
        for (var s : db.query(
            Schedule.class,
            "select s from Schedule s where s.weekday=?1 and s.offering.schoolClass.academicYear.id=?2",
            p.weekday(),
            o.schoolClass.academicYear.id
        )) {
            if (
                !s.id.equals(id) &&
                p.startTime().isBefore(s.endTime) &&
                p.endTime().isAfter(s.startTime) &&
                (s.offering.teacher.id.equals(o.teacher.id) ||
                    s.offering.schoolClass.id.equals(o.schoolClass.id) ||
                    s.room.equals(p.room()))
            ) throw bad("Schedule conflicts with a class, teacher or room");
        }
        var s = id == null ? new Schedule() : db.get(Schedule.class, id);
        s.offering = o;
        s.weekday = p.weekday();
        s.startTime = p.startTime();
        s.endTime = p.endTime();
        s.room = p.room();
        if (id == null) db.save(s);
        return scheduleView(s);
    }

    public List<View> schedules() {
        return db
            .query(Schedule.class, "select s from Schedule s order by s.weekday,s.startTime")
            .stream()
            .filter(s -> access.classVisible(s.offering.schoolClass))
            .map(this::scheduleView)
            .toList();
    }

    private View scheduleView(Schedule s) {
        return view(
            "id",
            s.id,
            "offeringId",
            s.offering.id,
            "className",
            s.offering.schoolClass.name,
            "subject",
            s.offering.subject.name,
            "weekday",
            s.weekday,
            "startTime",
            s.startTime,
            "endTime",
            s.endTime,
            "room",
            s.room
        );
    }

    private void dates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) throw bad("End date must follow start date");
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(BAD_REQUEST, message);
    }
}
