package com.schoolmanagement.common;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.mail.MailException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailure(MethodArgumentNotValidException exception) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Check the submitted fields.");
        p.setTitle("Validation failed");
        p.setProperty(
            "errors",
            exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e ->
                    Map.of(
                        "field",
                        e.getField(),
                        "message",
                        e.getDefaultMessage() == null ? "Invalid value" : e.getDefaultMessage()
                    )
                )
                .toList()
        );
        return p;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "A record with these identifiers already exists, or a related record prevents this change."
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail stale(ObjectOptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "This record changed. Refresh and try again."
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Files must be no larger than 10 MB."
        );
    }

    @ExceptionHandler(MailException.class)
    ProblemDetail mail(MailException e) {
        log.error("Account email delivery failed: {}", e.getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Email service is unavailable. Please try again later."
        );
    }
}
