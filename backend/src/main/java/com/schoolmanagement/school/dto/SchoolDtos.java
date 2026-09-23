package com.schoolmanagement.school.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class SchoolDtos {

    private SchoolDtos() {}

    public record ParentProfile(@Size(max = 500) String address) {}

    public record StudentInput(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @Size(max = 30) String gender,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 200) String emergencyContactName,
        @NotBlank @Size(max = 30) String emergencyContactPhone,
        Long parentId,
        Long userId
    ) {}

    public record Guardian(
        @Positive long parentId,
        @NotBlank @Size(max = 40) String relationship,
        boolean verified
    ) {}

    public record StudentStatus(
        @Pattern(regexp = "PENDING|ACTIVE|WITHDRAWN|GRADUATED") @NotNull String status
    ) {}

    public record TeacherInput(
        @Positive long userId,
        @NotBlank @Size(max = 40) String employeeNumber,
        @Size(max = 255) String qualification
    ) {}

    public record StaffInput(
        @Positive long userId,
        @NotBlank @Size(max = 40) String employeeNumber,
        @NotBlank @Size(max = 100) String jobTitle
    ) {}

    public record YearInput(
        @NotBlank @Size(max = 40) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
    ) {}

    public record TermInput(
        @Positive long academicYearId,
        @NotBlank @Size(max = 50) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
    ) {}

    public record ClassInput(
        @Positive long academicYearId,
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 30) String gradeLevel,
        @Min(1) @Max(1000) int capacity
    ) {}

    public record SubjectInput(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name
    ) {}

    public record OfferingInput(@Positive long classId, @Positive long subjectId, @Positive long teacherId) {}

    public record EnrollmentInput(@Positive long studentId, @Positive long classId) {}

    public record ScheduleInput(
        @Positive long offeringId,
        @Min(1) @Max(7) int weekday,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 80) String room
    ) {}

    public record AssignmentInput(
        @Positive long offeringId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotNull Instant dueAt
    ) {}

    public record GradeInput(
        @Positive long studentId,
        @Positive long offeringId,
        @Positive long termId,
        @Pattern(regexp = "ASSIGNMENT|QUIZ|EXAM|MIDTERM|FINAL") @NotNull String type,
        @NotBlank @Size(max = 200) String title,
        @NotNull @DecimalMin("0") @Digits(integer = 6, fraction = 2) BigDecimal score,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal maximum,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal weight
    ) {}

    public record AttendanceInput(
        @Positive long studentId,
        @Positive long classId,
        @NotNull @PastOrPresent LocalDate date,
        @Pattern(regexp = "PRESENT|ABSENT|LATE|EXCUSED") @NotNull String status
    ) {}

    public record FeeInput(
        @Positive long academicYearId,
        Long classId,
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Pattern(regexp = "[A-Z]{3}") @NotNull String currency
    ) {}

    public record InvoiceInput(
        @Positive long studentId,
        @Positive long feeStructureId,
        @NotNull LocalDate dueDate
    ) {}

    public record PaymentInput(
        @Positive long invoiceId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount
    ) {}

    public record AnnouncementInput(
        Long classId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String body,
        @Pattern(regexp = "NORMAL|IMPORTANT|EMERGENCY") @NotNull String priority
    ) {}

    public record MessageInput(@Positive long recipientId, @NotBlank @Size(max = 5000) String body) {}

    public record EventInput(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Pattern(regexp = "HOLIDAY|SCHOOL_DAY|CONFERENCE|EXAM|SPORT|TRIP|MEETING|GRADUATION|DEADLINE")
        @NotNull
        String category,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        boolean allDay
    ) {}

    public record SubmissionInput(@Positive long studentId, @NotBlank @Size(max = 10000) String body) {}

    public record PageView<T>(List<T> items, int page, int size, long totalElements, int totalPages) {}

    // Immutable response DTO; only explicitly selected scalar fields enter this record.
    public record View(@com.fasterxml.jackson.annotation.JsonAnyGetter Map<String, Object> fields) {
        public View {
            fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }
    }

    public static View view(Object... values) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]);
        return new View(map);
    }
}
