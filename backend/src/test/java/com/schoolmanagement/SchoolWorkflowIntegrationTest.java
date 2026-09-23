package com.schoolmanagement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.schoolmanagement.identity.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SchoolWorkflowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    IdentityService identity;

    @MockitoBean
    AccountMail mail;

    private static final String PASSWORD = "School-Test-Password-42";
    private String admin;

    @BeforeEach
    void setup() throws Exception {
        account("admin", "ADMINISTRATOR");
        admin = login("admin");
    }

    private UserAccount account(String name, String role) {
        return identity.create(name + "@example.test", PASSWORD, name, "Test", role, true);
    }

    private String login(String name) throws Exception {
        return postJson(
            "/api/auth/login",
            null,
            Map.of("email", name + "@example.test", "password", PASSWORD),
            200
        )
            .get("accessToken")
            .asText();
    }

    private JsonNode postJson(String path, String token, Object body, int status) throws Exception {
        return request(post(path), token, body, status);
    }

    private JsonNode request(
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
        String token,
        Object body,
        int status
    ) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        var response = mvc
            .perform(request)
            .andExpect(status().is(status))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }

    private long create(String path, Object body) throws Exception {
        return postJson("/api/" + path, admin, body, 201)
            .get("id")
            .asLong();
    }

    private Map<String, Object> student(long parentId) {
        return Map.of(
            "firstName",
            "Amina",
            "lastName",
            "Learner",
            "dateOfBirth",
            "2015-01-20",
            "emergencyContactName",
            "Guardian",
            "emergencyContactPhone",
            "555-0100",
            "parentId",
            parentId
        );
    }

    @Test
    void academicBillingAndCommunicationWorkflowEnforcesOwnership() throws Exception {
        var parent = account("parent", "PARENT");
        account("outsider", "PARENT");
        var teacher = account("teacher", "TEACHER");
        var otherTeacher = account("otherteacher", "TEACHER");
        var studentAccount = account("student", "STUDENT");
        String parentToken = login("parent"),
            outsider = login("outsider"),
            teacherToken = login("teacher"),
            otherTeacherToken = login("otherteacher"),
            studentToken = login("student");
        var parents = request(get("/api/parents"), admin, null, 200).get("items");
        long parentId = 0;
        for (var row : parents)
            if (row.get("userId").asLong() == parent.id) parentId = row.get("id").asLong();
        assertThat(parentId).isPositive();
        var input = new HashMap<>(student(parentId));
        input.put("userId", studentAccount.id);
        long studentId = create("students", input);
        long year = create(
            "academic-years",
            Map.of(
                "name",
                "2026-27",
                "startDate",
                LocalDate.now().withDayOfYear(1).toString(),
                "endDate",
                LocalDate.now().plusYears(1).withMonth(12).withDayOfMonth(31).toString()
            )
        );
        long term = create(
            "terms",
            Map.of(
                "academicYearId",
                year,
                "name",
                "Term One",
                "startDate",
                LocalDate.now().withDayOfYear(1).toString(),
                "endDate",
                LocalDate.now().withMonth(12).withDayOfMonth(31).toString()
            )
        );
        long classroom = create(
            "classes",
            Map.of("academicYearId", year, "name", "Grade 5A", "gradeLevel", "5", "capacity", 30)
        );
        long teacherId = create(
            "teachers",
            Map.of("userId", teacher.id, "employeeNumber", "T001", "qualification", "Mathematics")
        );
        create("teachers", Map.of("userId", otherTeacher.id, "employeeNumber", "T002"));
        long subject = create("subjects", Map.of("code", "MATH5", "name", "Mathematics"));
        long offering = create(
            "offerings",
            Map.of("classId", classroom, "subjectId", subject, "teacherId", teacherId)
        );
        create("enrollments", Map.of("studentId", studentId, "classId", classroom));
        postJson("/api/enrollments", admin, Map.of("studentId", studentId, "classId", classroom), 400);
        request(get("/api/students/" + studentId), parentToken, null, 200);
        request(get("/api/students/" + studentId), studentToken, null, 200);
        request(get("/api/students/" + studentId), outsider, null, 404);
        request(get("/api/students/" + studentId), otherTeacherToken, null, 404);
        assertThat(
            request(get("/api/dashboard"), teacherToken, null, 200).get("pendingGrades").asInt()
        ).isEqualTo(1);
        var grade = Map.of(
            "studentId",
            studentId,
            "offeringId",
            offering,
            "termId",
            term,
            "type",
            "EXAM",
            "title",
            "Algebra",
            "score",
            90,
            "maximum",
            100,
            "weight",
            1
        );
        postJson("/api/grades", parentToken, grade, 404);
        postJson("/api/grades", otherTeacherToken, grade, 404);
        postJson("/api/grades", teacherToken, grade, 201);
        assertThat(
            request(get("/api/dashboard"), teacherToken, null, 200).get("pendingGrades").asInt()
        ).isZero();
        assertThat(
            request(get("/api/grades/student/" + studentId + "/averages"), parentToken, null, 200)
                .get("overallAverage")
                .asDouble()
        ).isEqualTo(90);
        postJson(
            "/api/attendance",
            teacherToken,
            Map.of(
                "studentId",
                studentId,
                "classId",
                classroom,
                "date",
                LocalDate.now().toString(),
                "status",
                "PRESENT"
            ),
            201
        );
        postJson(
            "/api/attendance",
            teacherToken,
            Map.of(
                "studentId",
                studentId,
                "classId",
                classroom,
                "date",
                LocalDate.now().toString(),
                "status",
                "LATE"
            ),
            201
        );
        assertThat(
            request(get("/api/attendance/student/" + studentId), parentToken, null, 200).size()
        ).isEqualTo(1);
        long assignment = postJson(
            "/api/assignments",
            teacherToken,
            Map.of(
                "offeringId",
                offering,
                "title",
                "Fractions",
                "description",
                "Show your work",
                "dueAt",
                Instant.now().plusSeconds(86400).toString()
            ),
            201
        )
            .get("id")
            .asLong();
        postJson(
            "/api/assignments/" + assignment + "/submissions",
            studentToken,
            Map.of("studentId", studentId, "body", "My answer"),
            200
        );
        request(get("/api/assignments/" + assignment + "/submissions"), outsider, null, 404);
        long fee = create(
            "fee-structures",
            Map.of(
                "academicYearId",
                year,
                "classId",
                classroom,
                "name",
                "Tuition",
                "amount",
                500,
                "currency",
                "USD"
            )
        );
        long invoice = create(
            "invoices",
            Map.of("studentId", studentId, "feeStructureId", fee, "dueDate", "2026-12-01")
        );
        String key = UUID.randomUUID().toString();
        var paymentBody = Map.of("invoiceId", invoice, "amount", 100);
        request(post("/api/payments").header("Idempotency-Key", key), outsider, paymentBody, 404);
        var payment = request(
            post("/api/payments").header("Idempotency-Key", key),
            parentToken,
            paymentBody,
            201
        );
        var retry = request(
            post("/api/payments").header("Idempotency-Key", key),
            parentToken,
            paymentBody,
            201
        );
        assertThat(retry.get("id")).isEqualTo(payment.get("id"));
        request(
            post("/api/payments").header("Idempotency-Key", UUID.randomUUID().toString()),
            parentToken,
            Map.of("invoiceId", invoice, "amount", 401),
            400
        );
        assertThat(
            request(get("/api/fees/student/" + studentId), parentToken, null, 200)
                .get(0)
                .get("balance")
                .asDouble()
        ).isEqualTo(400);
        mvc.perform(
            get("/api/receipts/" + payment.get("receiptId").asLong()).header(
                "Authorization",
                "Bearer " + parentToken
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("No real money")));
        postJson(
            "/api/announcements",
            teacherToken,
            Map.of(
                "classId",
                classroom,
                "title",
                "Exam reminder",
                "body",
                "Please revise",
                "priority",
                "IMPORTANT"
            ),
            201
        );
        assertThat(
            request(get("/api/notifications"), parentToken, null, 200).get("totalElements").asInt()
        ).isEqualTo(1);
        assertThat(
            request(get("/api/notifications"), outsider, null, 200).get("totalElements").asInt()
        ).isZero();
        postJson("/api/messages", parentToken, Map.of("recipientId", teacher.id, "body", "Thank you"), 201);
        postJson("/api/messages", outsider, Map.of("recipientId", teacher.id, "body", "Unauthorized"), 403);
        request(get("/api/dashboard"), parentToken, null, 200);
        request(get("/api/dashboard"), teacherToken, null, 200);
        request(get("/api/reports/payments"), parentToken, null, 403);
    }

    @Test
    void registrationVerificationResetAndRevocation() throws Exception {
        postJson(
            "/api/auth/register",
            null,
            Map.of(
                "email",
                "newparent@example.test",
                "password",
                PASSWORD,
                "firstName",
                "New",
                "lastName",
                "Parent",
                "role",
                "ADMINISTRATOR"
            ),
            201
        );
        var token = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("newparent@example.test"), token.capture(), eq("EMAIL_VERIFICATION"));
        postJson(
            "/api/auth/login",
            null,
            Map.of("email", "newparent@example.test", "password", PASSWORD),
            401
        );
        postJson("/api/auth/verify-email", null, Map.of("token", token.getValue()), 200);
        postJson("/api/auth/verify-email", null, Map.of("token", token.getValue()), 400);
        String jwt = login("newparent");
        var me = request(get("/api/auth/me"), jwt, null, 200);
        assertThat(me.get("roles").toString()).isEqualTo("[\"PARENT\"]");
        postJson("/api/auth/forgot-password", null, Map.of("email", "newparent@example.test"), 200);
        verify(mail).send(eq("newparent@example.test"), token.capture(), eq("PASSWORD_RESET"));
        postJson(
            "/api/auth/reset-password",
            null,
            Map.of("token", token.getValue(), "password", "New-Password-For-School-55"),
            200
        );
        request(get("/api/auth/me"), jwt, null, 401);
        String updated = postJson(
            "/api/auth/login",
            null,
            Map.of("email", "newparent@example.test", "password", "New-Password-For-School-55"),
            200
        )
            .get("accessToken")
            .asText();
        postJson("/api/auth/logout", updated, Map.of(), 200);
        request(get("/api/auth/me"), updated, null, 401);
    }

    @Test
    void deactivationAndMalformedTokensAreRejected() throws Exception {
        var parent = account("deactivate", "PARENT");
        String token = login("deactivate");
        request(
            put("/api/users/" + parent.id + "/access"),
            admin,
            Map.of("active", false, "roles", List.of("PARENT")),
            200
        );
        request(get("/api/students"), token, null, 401);
        request(get("/api/students"), "not-a-jwt", null, 401);
    }

    @Test
    void teacherCannotReadAnotherClassThroughSharedEnrollmentHistory() throws Exception {
        var firstTeacher = account("scope-teacher-one", "TEACHER");
        var secondTeacher = account("scope-teacher-two", "TEACHER");
        var student = Map.of(
            "firstName",
            "Shared",
            "lastName",
            "Student",
            "dateOfBirth",
            "2014-01-01",
            "emergencyContactName",
            "Guardian",
            "emergencyContactPhone",
            "555-0100"
        );
        long studentId = create("students", student);
        long yearOne = create(
            "academic-years",
            Map.of(
                "name",
                "Scope Year One",
                "startDate",
                LocalDate.now().minusYears(1).withDayOfYear(1).toString(),
                "endDate",
                LocalDate.now().minusYears(1).withMonth(12).withDayOfMonth(31).toString()
            )
        );
        long yearTwo = create(
            "academic-years",
            Map.of(
                "name",
                "Scope Year Two",
                "startDate",
                LocalDate.now().withDayOfYear(1).toString(),
                "endDate",
                LocalDate.now().withMonth(12).withDayOfMonth(31).toString()
            )
        );
        long classOne = create(
            "classes",
            Map.of("academicYearId", yearOne, "name", "Scope Class One", "gradeLevel", "4", "capacity", 20)
        );
        long classTwo = create(
            "classes",
            Map.of("academicYearId", yearTwo, "name", "Scope Class Two", "gradeLevel", "5", "capacity", 20)
        );
        long teacherOne = create("teachers", Map.of("userId", firstTeacher.id, "employeeNumber", "SCOPE-1"));
        long teacherTwo = create("teachers", Map.of("userId", secondTeacher.id, "employeeNumber", "SCOPE-2"));
        long subject = create("subjects", Map.of("code", "SCOPE", "name", "Scope Subject"));
        create("offerings", Map.of("classId", classOne, "subjectId", subject, "teacherId", teacherOne));
        long otherOffering = create(
            "offerings",
            Map.of("classId", classTwo, "subjectId", subject, "teacherId", teacherTwo)
        );
        create("enrollments", Map.of("studentId", studentId, "classId", classOne));
        create("enrollments", Map.of("studentId", studentId, "classId", classTwo));
        long otherAssignment = create(
            "assignments",
            Map.of(
                "offeringId",
                otherOffering,
                "title",
                "Private assignment",
                "description",
                "Only assigned class",
                "dueAt",
                Instant.now().plusSeconds(86400).toString()
            )
        );
        String token = login("scope-teacher-one");
        assertThat(request(get("/api/classes"), token, null, 200).get("totalElements").asInt()).isEqualTo(1);
        request(get("/api/assignments/" + otherAssignment), token, null, 404);
    }

    @Autowired
    org.springframework.security.oauth2.jwt.JwtEncoder encoder;

    @Test
    void signedTokensWithExpiredTimeOrWrongAudienceAreRejected() throws Exception {
        var user = account("claims", "PARENT");
        for (boolean expired : List.of(true, false)) {
            var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer("school-management")
                .subject(user.id.toString())
                .audience(List.of(expired ? "school-api" : "another-service"))
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(expired ? Instant.now().minusSeconds(3600) : Instant.now().plusSeconds(1800))
                .claim("version", 0)
                .build();
            var header = org.springframework.security.oauth2.jwt.JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256
            ).build();
            String jwt = encoder
                .encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header, claims))
                .getTokenValue();
            request(get("/api/auth/me"), jwt, null, 401);
        }
    }
}
