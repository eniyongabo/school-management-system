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
public class PeopleService {

    private final SchoolRepository db;
    private final CurrentUser current;
    private final SchoolAccess access;

    public PeopleService(SchoolRepository d, CurrentUser c, SchoolAccess a) {
        db = d;
        current = c;
        access = a;
    }

    public Parent parent(UserAccount u) {
        var found = db.query(Parent.class, "select p from Parent p where p.user.id=?1", u.id);
        if (!found.isEmpty()) return found.get(0);
        var p = new Parent();
        p.user = u;
        return db.save(p);
    }

    public View create(StudentInput input) {
        var u = current.require("PARENT", "ADMINISTRATOR");
        var s = new Student();
        s.studentNumber = "ST-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        s.firstName = input.firstName();
        s.lastName = input.lastName();
        s.dateOfBirth = input.dateOfBirth();
        s.gender = input.gender();
        s.address = input.address();
        s.emergencyContactName = input.emergencyContactName();
        s.emergencyContactPhone = input.emergencyContactPhone();
        s.enrollmentDate = LocalDate.now();
        s.status = u.has("ADMINISTRATOR") ? "ACTIVE" : "PENDING";
        if (input.userId() != null) {
            current.require("ADMINISTRATOR");
            s.user = db.get(UserAccount.class, input.userId());
            if (!s.user.has("STUDENT")) throw new ResponseStatusException(
                BAD_REQUEST,
                "Account must have student role"
            );
        }
        db.save(s);
        Parent guardian = u.has("ADMINISTRATOR")
            ? input.parentId() == null
                ? null
                : db.get(Parent.class, input.parentId())
            : parent(u);
        if (guardian != null) db.sql(
            "insert into parent_students(parent_id,student_id,relationship,verified) values(?1,?2,?3,?4)",
            guardian.id,
            s.id,
            "Guardian",
            u.has("ADMINISTRATOR")
        );
        return studentView(s);
    }

    public View updateStudent(long id, StudentInput input) {
        current.require("ADMINISTRATOR");
        var s = db.get(Student.class, id);
        s.firstName = input.firstName();
        s.lastName = input.lastName();
        s.dateOfBirth = input.dateOfBirth();
        s.gender = input.gender();
        s.address = input.address();
        s.emergencyContactName = input.emergencyContactName();
        s.emergencyContactPhone = input.emergencyContactPhone();
        if (input.userId() != null) {
            var u = db.get(UserAccount.class, input.userId());
            if (!u.has("STUDENT")) throw new ResponseStatusException(
                BAD_REQUEST,
                "Account must have student role"
            );
            s.user = u;
        }
        return studentView(s);
    }

    public View studentView(Student s) {
        return view(
            "id",
            s.id,
            "studentNumber",
            s.studentNumber,
            "firstName",
            s.firstName,
            "lastName",
            s.lastName,
            "dateOfBirth",
            s.dateOfBirth,
            "gender",
            s.gender,
            "address",
            s.address,
            "emergencyContactName",
            s.emergencyContactName,
            "emergencyContactPhone",
            s.emergencyContactPhone,
            "enrollmentDate",
            s.enrollmentDate,
            "status",
            s.status,
            "userId",
            s.user == null ? null : s.user.id
        );
    }

    public List<View> students() {
        return db
            .query(Student.class, "select s from Student s order by s.lastName,s.firstName")
            .stream()
            .filter(access::studentVisible)
            .map(this::studentView)
            .toList();
    }

    public View student(long id) {
        return studentView(access.student(id, false));
    }

    public List<View> children(long parentId) {
        var p = db.get(Parent.class, parentId);
        var u = current.get();
        if (!u.has("ADMINISTRATOR") && !p.user.id.equals(u.id)) throw SchoolAccess.hidden();
        return db
            .query(Student.class, "select s from Student s order by s.id")
            .stream()
            .filter(
                s ->
                    db.scalar(
                        "select count(*) from parent_students where parent_id=?1 and student_id=?2 and verified=true",
                        p.id,
                        s.id
                    ) > 0
            )
            .map(this::studentView)
            .toList();
    }

    public List<View> parents() {
        var u = current.require("ADMINISTRATOR", "PARENT");
        if (u.has("PARENT")) parent(u);
        return db
            .query(Parent.class, "select p from Parent p order by p.id")
            .stream()
            .filter(p -> u.has("ADMINISTRATOR") || p.user.id.equals(u.id))
            .map(p ->
                view(
                    "id",
                    p.id,
                    "userId",
                    p.user.id,
                    "name",
                    p.user.firstName + " " + p.user.lastName,
                    "email",
                    p.user.email,
                    "address",
                    p.address
                )
            )
            .toList();
    }

    public View status(long id, StudentStatus input) {
        current.require("ADMINISTRATOR");
        var s = db.get(Student.class, id);
        s.status = input.status();
        return studentView(s);
    }

    public void guardian(long id, Guardian input) {
        current.require("ADMINISTRATOR");
        db.get(Student.class, id);
        var p = db.get(Parent.class, input.parentId());
        db.sql("delete from parent_students where parent_id=?1 and student_id=?2", p.id, id);
        db.sql(
            "insert into parent_students(parent_id,student_id,relationship,verified) values(?1,?2,?3,?4)",
            p.id,
            id,
            input.relationship(),
            input.verified()
        );
    }

    public List<View> guardians(long id) {
        current.require("ADMINISTRATOR");
        return db
            .query(Parent.class, "select p from Parent p")
            .stream()
            .filter(
                p ->
                    db.scalar(
                        "select count(*) from parent_students where parent_id=?1 and student_id=?2",
                        p.id,
                        id
                    ) > 0
            )
            .map(p ->
                view(
                    "parentId",
                    p.id,
                    "name",
                    p.user.firstName + " " + p.user.lastName,
                    "verified",
                    db.scalar(
                        "select count(*) from parent_students where parent_id=?1 and student_id=?2 and verified=true",
                        p.id,
                        id
                    ) > 0
                )
            )
            .toList();
    }

    public void parentProfile(long id, ParentProfile input) {
        var parent = db.get(Parent.class, id);
        var user = current.get();
        if (
            !user.has("ADMINISTRATOR") && (!user.has("PARENT") || !parent.user.id.equals(user.id))
        ) throw SchoolAccess.hidden();
        parent.address = input.address();
    }

    public View teacher(TeacherInput input) {
        return teacher(null, input);
    }

    public View teacher(Long id, TeacherInput input) {
        current.require("ADMINISTRATOR");
        var t = id == null ? new Teacher() : db.get(Teacher.class, id);
        t.user = db.get(UserAccount.class, input.userId());
        if (!t.user.has("TEACHER")) throw new ResponseStatusException(
            BAD_REQUEST,
            "Account must have teacher role"
        );
        t.employeeNumber = input.employeeNumber();
        t.qualification = input.qualification();
        if (id == null) db.save(t);
        return teacherView(t);
    }

    public View teacherView(Teacher t) {
        return view(
            "id",
            t.id,
            "userId",
            t.user.id,
            "name",
            t.user.firstName + " " + t.user.lastName,
            "email",
            t.user.email,
            "employeeNumber",
            t.employeeNumber,
            "qualification",
            t.qualification
        );
    }

    public List<View> teachers() {
        var u = current.get();
        return db
            .query(Teacher.class, "select t from Teacher t order by t.id")
            .stream()
            .filter(
                t ->
                    u.has("ADMINISTRATOR") ||
                    t.user.id.equals(u.id) ||
                    db
                        .query(ClassSubject.class, "select o from ClassSubject o where o.teacher.id=?1", t.id)
                        .stream()
                        .anyMatch(o -> access.classVisible(o.schoolClass))
            )
            .map(this::teacherView)
            .toList();
    }

    public View staff(StaffInput input) {
        return staff(null, input);
    }

    public View staff(Long id, StaffInput input) {
        current.require("ADMINISTRATOR");
        var s = id == null ? new Staff() : db.get(Staff.class, id);
        s.user = db.get(UserAccount.class, input.userId());
        if (!s.user.has("ACCOUNTANT")) throw new ResponseStatusException(
            BAD_REQUEST,
            "Account must have accountant role"
        );
        s.employeeNumber = input.employeeNumber();
        s.jobTitle = input.jobTitle();
        if (id == null) db.save(s);
        return staffView(s);
    }

    public List<View> staff() {
        current.require("ADMINISTRATOR");
        return db
            .query(Staff.class, "select s from Staff s order by s.id")
            .stream()
            .map(this::staffView)
            .toList();
    }

    private View staffView(Staff s) {
        return view(
            "id",
            s.id,
            "userId",
            s.user.id,
            "name",
            s.user.firstName + " " + s.user.lastName,
            "employeeNumber",
            s.employeeNumber,
            "jobTitle",
            s.jobTitle
        );
    }
}
