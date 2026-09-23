package com.schoolmanagement.school.controller;

import static com.schoolmanagement.school.dto.SchoolDtos.*;

import com.schoolmanagement.school.service.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class SchoolController {

    private final PeopleService people;
    private final AcademicService academics;
    private final LearningService learning;
    private final BillingService billing;
    private final CommunicationService comms;
    private final ReportService reports;

    public SchoolController(
        PeopleService p,
        AcademicService a,
        LearningService l,
        BillingService b,
        CommunicationService c,
        ReportService r
    ) {
        people = p;
        academics = a;
        learning = l;
        billing = b;
        comms = c;
        reports = r;
    }

    @GetMapping("/students")
    public PageView<View> students(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(people.students(), page, size, search, sort, direction);
    }

    @GetMapping("/parents")
    public PageView<View> parents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(people.parents(), page, size, search, sort, direction);
    }

    @GetMapping("/teachers")
    public PageView<View> teachers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(people.teachers(), page, size, search, sort, direction);
    }

    @GetMapping("/staff")
    public PageView<View> staff(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(people.staff(), page, size, search, sort, direction);
    }

    @GetMapping("/academic-years")
    public PageView<View> academic_years(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.years(), page, size, search, sort, direction);
    }

    @GetMapping("/terms")
    public PageView<View> terms(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.terms(), page, size, search, sort, direction);
    }

    @GetMapping("/classes")
    public PageView<View> classes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.classes(), page, size, search, sort, direction);
    }

    @GetMapping("/subjects")
    public PageView<View> subjects(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.subjects(), page, size, search, sort, direction);
    }

    @GetMapping("/offerings")
    public PageView<View> offerings(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.offerings(), page, size, search, sort, direction);
    }

    @GetMapping("/enrollments")
    public PageView<View> enrollments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.enrollments(), page, size, search, sort, direction);
    }

    @GetMapping("/schedules")
    public PageView<View> schedules(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(academics.schedules(), page, size, search, sort, direction);
    }

    @GetMapping("/assignments")
    public PageView<View> assignments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(learning.assignments(), page, size, search, sort, direction);
    }

    @GetMapping("/grades")
    public PageView<View> grades(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(learning.grades(null), page, size, search, sort, direction);
    }

    @GetMapping("/attendance")
    public PageView<View> attendance(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(learning.attendance((Long) null), page, size, search, sort, direction);
    }

    @GetMapping("/fee-structures")
    public PageView<View> fee_structures(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(billing.fees(), page, size, search, sort, direction);
    }

    @GetMapping("/invoices")
    public PageView<View> invoices(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(billing.invoices(null), page, size, search, sort, direction);
    }

    @GetMapping("/payments")
    public PageView<View> payments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(billing.payments(), page, size, search, sort, direction);
    }

    @GetMapping("/announcements")
    public PageView<View> announcements(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(comms.announcements(), page, size, search, sort, direction);
    }

    @GetMapping("/notifications")
    public PageView<View> notifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(comms.notifications(), page, size, search, sort, direction);
    }

    @GetMapping("/messages")
    public PageView<View> messages(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(comms.messages(), page, size, search, sort, direction);
    }

    @GetMapping("/contacts")
    public PageView<View> contacts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(comms.contacts(), page, size, search, sort, direction);
    }

    @GetMapping("/events")
    public PageView<View> events(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "id") String sort,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        return page(comms.events(), page, size, search, sort, direction);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_students(@Valid @RequestBody StudentInput p) {
        return people.create(p);
    }

    @PostMapping("/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_teachers(@Valid @RequestBody TeacherInput p) {
        return people.teacher(p);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_staff(@Valid @RequestBody StaffInput p) {
        return people.staff(p);
    }

    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_academic_years(@Valid @RequestBody YearInput p) {
        return academics.year(p);
    }

    @PostMapping("/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_terms(@Valid @RequestBody TermInput p) {
        return academics.term(p);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_classes(@Valid @RequestBody ClassInput p) {
        return academics.schoolClass(p);
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_subjects(@Valid @RequestBody SubjectInput p) {
        return academics.subject(p);
    }

    @PostMapping("/offerings")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_offerings(@Valid @RequestBody OfferingInput p) {
        return academics.offering(p);
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_enrollments(@Valid @RequestBody EnrollmentInput p) {
        return academics.enroll(p);
    }

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_schedules(@Valid @RequestBody ScheduleInput p) {
        return academics.schedule(p);
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_assignments(@Valid @RequestBody AssignmentInput p) {
        return learning.assignment(p);
    }

    @PostMapping("/grades")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_grades(@Valid @RequestBody GradeInput p) {
        return learning.grade(null, p);
    }

    @PostMapping("/attendance")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_attendance(@Valid @RequestBody AttendanceInput p) {
        return learning.attendance(p);
    }

    @PostMapping("/fee-structures")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_fee_structures(@Valid @RequestBody FeeInput p) {
        return billing.fee(p);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_invoices(@Valid @RequestBody InvoiceInput p) {
        return billing.invoice(p);
    }

    @PostMapping("/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_announcements(@Valid @RequestBody AnnouncementInput p) {
        return comms.announce(p);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_messages(@Valid @RequestBody MessageInput p) {
        return comms.send(p);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public View create_events(@Valid @RequestBody EventInput p) {
        return comms.event(null, p);
    }

    @PutMapping("/academic-years/{id}")
    public View update_year(@PathVariable long id, @Valid @RequestBody YearInput p) {
        return academics.year(id, p);
    }

    @PutMapping("/terms/{id}")
    public View update_term(@PathVariable long id, @Valid @RequestBody TermInput p) {
        return academics.term(id, p);
    }

    @PutMapping("/classes/{id}")
    public View update_schoolClass(@PathVariable long id, @Valid @RequestBody ClassInput p) {
        return academics.schoolClass(id, p);
    }

    @PutMapping("/subjects/{id}")
    public View update_subject(@PathVariable long id, @Valid @RequestBody SubjectInput p) {
        return academics.subject(id, p);
    }

    @PutMapping("/offerings/{id}")
    public View update_offering(@PathVariable long id, @Valid @RequestBody OfferingInput p) {
        return academics.offering(id, p);
    }

    @PutMapping("/schedules/{id}")
    public View update_schedule(@PathVariable long id, @Valid @RequestBody ScheduleInput p) {
        return academics.schedule(id, p);
    }

    @PutMapping("/students/{id}")
    public View updateStudent(@PathVariable long id, @Valid @RequestBody StudentInput p) {
        return people.updateStudent(id, p);
    }

    @PutMapping("/assignments/{id}")
    public View updateAssignment(@PathVariable long id, @Valid @RequestBody AssignmentInput p) {
        return learning.assignment(id, p);
    }

    @GetMapping("/assignments/{id}")
    public View getAssignment(@PathVariable long id) {
        return learning.assignment(id);
    }

    @PutMapping("/fee-structures/{id}")
    public View updateFee(@PathVariable long id, @Valid @RequestBody FeeInput p) {
        return billing.fee(id, p);
    }

    @PutMapping("/parents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void parentProfile(@PathVariable long id, @Valid @RequestBody ParentProfile input) {
        people.parentProfile(id, input);
    }

    @PutMapping("/teachers/{id}")
    public View teacherProfile(@PathVariable long id, @Valid @RequestBody TeacherInput input) {
        return people.teacher(id, input);
    }

    @PutMapping("/staff/{id}")
    public View staffProfile(@PathVariable long id, @Valid @RequestBody StaffInput input) {
        return people.staff(id, input);
    }

    @GetMapping("/students/{id}")
    public View student(@PathVariable long id) {
        return people.student(id);
    }

    @GetMapping("/parents/{id}/children")
    public List<View> children(@PathVariable long id) {
        return people.children(id);
    }

    @PutMapping("/students/{id}/status")
    public View status(@PathVariable long id, @Valid @RequestBody StudentStatus p) {
        return people.status(id, p);
    }

    @GetMapping("/students/{id}/guardians")
    public List<View> guardians(@PathVariable long id) {
        return people.guardians(id);
    }

    @PutMapping("/students/{id}/guardians")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void guardian(@PathVariable long id, @Valid @RequestBody Guardian p) {
        people.guardian(id, p);
    }

    @DeleteMapping("/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable long id) {
        academics.withdraw(id);
    }

    @PutMapping("/grades/{id}")
    public View grade(@PathVariable long id, @Valid @RequestBody GradeInput p) {
        return learning.grade(id, p);
    }

    @GetMapping("/grades/student/{id}")
    public List<View> studentGrades(@PathVariable long id) {
        return learning.grades(id);
    }

    @GetMapping("/grades/student/{id}/averages")
    public View averages(@PathVariable long id) {
        return learning.averages(id);
    }

    @GetMapping("/attendance/student/{id}")
    public List<View> studentAttendance(@PathVariable long id) {
        return learning.attendance(id);
    }

    @GetMapping("/fees/student/{id}")
    public List<View> studentFees(@PathVariable long id) {
        return billing.invoices(id);
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public View pay(@Valid @RequestBody PaymentInput p, @RequestHeader("Idempotency-Key") String key) {
        return billing.pay(p, key);
    }

    @GetMapping("/receipts/{id}")
    public ResponseEntity<byte[]> receipt(@PathVariable long id) {
        return download(
            "receipt-" + id + ".txt",
            billing.receipt(id).getBytes(StandardCharsets.UTF_8),
            "text/plain;charset=UTF-8"
        );
    }

    @GetMapping("/dashboard")
    public View dashboard() {
        return reports.dashboard();
    }

    @PutMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readNotification(@PathVariable long id) {
        comms.readNotification(id);
    }

    @PutMapping("/messages/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readMessage(@PathVariable long id) {
        comms.readMessage(id);
    }

    @PutMapping("/events/{id}")
    public View updateEvent(@PathVariable long id, @Valid @RequestBody EventInput p) {
        return comms.event(id, p);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable long id) {
        comms.deleteEvent(id);
    }

    @GetMapping("/assignments/{id}/submissions")
    public List<View> submissions(@PathVariable long id) {
        return learning.submissions(id);
    }

    @PostMapping("/assignments/{id}/submissions")
    public View submit(@PathVariable long id, @Valid @RequestBody SubmissionInput p) {
        return learning.submit(id, p);
    }

    @GetMapping("/assignments/{id}/documents")
    public List<View> documents(@PathVariable long id) {
        return learning.documents(id);
    }

    @PostMapping("/assignments/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public View upload(@PathVariable long id, @RequestParam MultipartFile file) {
        return learning.upload(id, file);
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<byte[]> document(@PathVariable long id) {
        var d = learning.download(id);
        return download(d.filename(), d.bytes(), "application/octet-stream");
    }

    @GetMapping("/reports/{type}")
    public List<View> report(@PathVariable String type) {
        return reports.report(type);
    }

    @GetMapping("/reports/{type}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type) {
        return download(
            type + ".csv",
            reports.csv(reports.report(type)).getBytes(StandardCharsets.UTF_8),
            "text/csv;charset=UTF-8"
        );
    }

    @GetMapping("/students/{id}/report-card")
    public ResponseEntity<byte[]> reportCard(@PathVariable long id) {
        return download(
            "report-card-" + id + ".txt",
            reports.reportCard(id).getBytes(StandardCharsets.UTF_8),
            "text/plain;charset=UTF-8"
        );
    }

    private ResponseEntity<byte[]> download(String filename, byte[] bytes, String mediaType) {
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString()
            )
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.parseMediaType(mediaType))
            .body(bytes);
    }

    private PageView<View> page(
        List<View> rows,
        int page,
        int size,
        String search,
        String sort,
        String direction
    ) {
        if (
            page < 0 ||
            size < 1 ||
            size > 100 ||
            !Set.of(
                "id",
                "name",
                "firstName",
                "lastName",
                "title",
                "dueDate",
                "dueAt",
                "startAt",
                "status",
                "amount",
                "date"
            ).contains(sort) ||
            !Set.of("asc", "desc").contains(direction)
        ) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination or sorting");
        var filtered = rows
            .stream()
            .filter(
                v ->
                    search.isBlank() ||
                    v
                        .fields()
                        .values()
                        .stream()
                        .anyMatch(
                            value ->
                                value != null &&
                                value
                                    .toString()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(search.toLowerCase(Locale.ROOT))
                        )
            )
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Comparator<View> comparator = (a, b) -> compare(a.fields().get(sort), b.fields().get(sort));
        if (direction.equals("desc")) comparator = comparator.reversed();
        filtered.sort(comparator);
        int from = (int) Math.min((long) page * size, filtered.size()),
            to = Math.min(from + size, filtered.size());
        return new PageView<>(
            filtered.subList(from, to),
            page,
            size,
            filtered.size(),
            (filtered.size() + size - 1) / size
        );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int compare(Object a, Object b) {
        if (a == null) return b == null ? 0 : 1;
        if (b == null) return -1;
        if (a instanceof Comparable && a.getClass().equals(b.getClass())) return ((Comparable) a).compareTo(
            b
        );
        return a.toString().compareToIgnoreCase(b.toString());
    }
}
