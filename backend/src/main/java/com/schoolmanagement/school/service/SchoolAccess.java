package com.schoolmanagement.school.service;

import static org.springframework.http.HttpStatus.*;

import com.schoolmanagement.identity.*;
import com.schoolmanagement.school.entity.*;
import com.schoolmanagement.school.repository.SchoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SchoolAccess {

    private final SchoolRepository db;
    private final CurrentUser current;

    public SchoolAccess(SchoolRepository db, CurrentUser current) {
        this.db = db;
        this.current = current;
    }

    public boolean studentVisible(Student s) {
        return studentVisible(current.get(), s);
    }

    public boolean studentVisible(UserAccount u, Student s) {
        if (u.has("ADMINISTRATOR") || u.has("ACCOUNTANT")) return true;
        if (u.has("STUDENT") && s.user != null && s.user.id.equals(u.id)) return true;
        if (
            u.has("PARENT") &&
            db.scalar(
                "select count(*) from parent_students ps join parents p on p.id=ps.parent_id where p.user_id=?1 and ps.student_id=?2 and ps.verified=true",
                u.id,
                s.id
            ) > 0
        ) return true;
        return (
            u.has("TEACHER") &&
            db.count(
                "select count(e) from Enrollment e where e.student.id=?1 and e.schoolClass.id in (select c.schoolClass.id from ClassSubject c where c.teacher.user.id=?2)",
                s.id,
                u.id
            ) > 0
        );
    }

    public Student student(long id, boolean academic) {
        var s = db.get(Student.class, id);
        if (!studentVisible(s)) throw hidden();
        if (
            academic && current.get().has("ACCOUNTANT") && !current.get().has("ADMINISTRATOR")
        ) throw new ResponseStatusException(FORBIDDEN);
        return s;
    }

    public boolean classVisible(SchoolClass c) {
        return classVisible(current.get(), c);
    }

    public boolean classVisible(UserAccount u, SchoolClass c) {
        if (u.has("ADMINISTRATOR") || u.has("ACCOUNTANT")) return true;
        if (
            u.has("TEACHER") &&
            db.count(
                "select count(o) from ClassSubject o where o.schoolClass.id=?1 and o.teacher.user.id=?2",
                c.id,
                u.id
            ) > 0
        ) return true;
        // A shared student must not give a teacher access to the student's other classes.
        if (!u.has("PARENT") && !u.has("STUDENT")) return false;
        return db
            .query(Enrollment.class, "select e from Enrollment e where e.schoolClass.id=?1", c.id)
            .stream()
            .anyMatch(e -> familyStudentVisible(u, e.student));
    }

    private boolean familyStudentVisible(UserAccount user, Student student) {
        if (user.has("STUDENT") && student.user != null && student.user.id.equals(user.id)) return true;
        return (
            user.has("PARENT") &&
            db.scalar(
                "select count(*) from parent_students ps join parents p on p.id=ps.parent_id where p.user_id=?1 and ps.student_id=?2 and ps.verified=true",
                user.id,
                student.id
            ) > 0
        );
    }

    public ClassSubject offering(long id, boolean write) {
        var o = db.get(ClassSubject.class, id);
        var u = current.get();
        if (write) {
            if (
                !u.has("ADMINISTRATOR") && !(u.has("TEACHER") && o.teacher.user.id.equals(u.id))
            ) throw hidden();
        } else {
            current.require("ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT");
            if (!classVisible(o.schoolClass)) throw hidden();
        }
        return o;
    }

    public void classWrite(long id) {
        var u = current.get();
        if (
            !u.has("ADMINISTRATOR") &&
            !(
                u.has("TEACHER") &&
                db.count(
                    "select count(c) from ClassSubject c where c.schoolClass.id=?1 and c.teacher.user.id=?2",
                    id,
                    u.id
                ) > 0
            )
        ) throw hidden();
    }

    public void enrolled(Student s, SchoolClass c) {
        if (
            db.count(
                "select count(e) from Enrollment e where e.student.id=?1 and e.schoolClass.id=?2 and e.withdrawnOn is null",
                s.id,
                c.id
            ) == 0
        ) throw new ResponseStatusException(BAD_REQUEST, "Student is not enrolled in this class");
    }

    public boolean canMessage(UserAccount a, UserAccount b) {
        if (a.id.equals(b.id) || !b.active) return false;
        if (
            (a.has("ADMINISTRATOR") && b.has("TEACHER")) || (a.has("TEACHER") && b.has("ADMINISTRATOR"))
        ) return true;
        UserAccount p = a.has("PARENT") ? a : b.has("PARENT") ? b : null;
        UserAccount t = a.has("TEACHER") ? a : b.has("TEACHER") ? b : null;
        return (
            p != null &&
            t != null &&
            db.scalar(
                "select count(*) from parent_students ps join parents p on p.id=ps.parent_id join enrollments e on e.student_id=ps.student_id join class_subjects c on c.class_id=e.class_id join teachers t on t.id=c.teacher_id where p.user_id=?1 and t.user_id=?2 and ps.verified=true and e.withdrawn_on is null",
                p.id,
                t.id
            ) > 0
        );
    }

    public static ResponseStatusException hidden() {
        return new ResponseStatusException(NOT_FOUND, "Record not found");
    }
}
