package com.example.SecurityAdvance.exceptions;

import com.example.SecurityAdvance.configs.Translator;
import com.example.SecurityAdvance.dtos.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // 1. Business Exception (custom AppException)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, WebRequest request) {
        String localizedMessage = Translator.toLocale(e.getMessageKey());

        // Log theo level phù hợp
        if (e.getStatus().is5xxServerError()) {
            log.error("AppException [{}]: {} at {}", e.getStatus(), localizedMessage, extractPath(request));
        } else {
            log.warn("AppException [{}]: {} at {}", e.getStatus(), localizedMessage, extractPath(request));
        }

        ErrorResponse response = ErrorResponse.builder()
                .code(e.getStatus().value())
                .message(localizedMessage)
                .build();

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    // 2. Validation Exception - Trả errors map chi tiết
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, WebRequest request) {

        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        log.warn("Validation failed at {}: {}", extractPath(request), errors);

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(Translator.toLocale("err.validation"))
                .errors(errors.isEmpty() ? null : errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // 3. DataIntegrityViolationException - Lỗi constraint DB (unique, foreign key, not null)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e, WebRequest request) {

        String message;
        String constraintName = extractConstraintName(e);

        log.error("Data integrity violation at {}: constraint [{}], error: {}",
                extractPath(request), constraintName, e.getMessage());

        // Parse message dựa vào constraint name hoặc error message
        if (e.getMessage() != null) {
            String errorMsg = e.getMessage().toLowerCase();

            if (errorMsg.contains("duplicate") || errorMsg.contains("unique")) {
                // Xác định field bị duplicate dựa vào constraint name
                message = parseUniqueConstraintError(constraintName);
            } else if (errorMsg.contains("foreign key") || errorMsg.contains("cannot delete")) {
                message = Translator.toLocale("err.foreign_key_violation");
            } else if (errorMsg.contains("cannot be null") || errorMsg.contains("not null")) {
                message = Translator.toLocale("err.required_field_missing");
            } else {
                message = Translator.toLocale("err.data_integrity");
            }
        } else {
            message = Translator.toLocale("err.data_integrity");
        }

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.CONFLICT.value())
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 4. Access Denied Exception (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e, WebRequest request) {

        log.warn("Access denied at {}: {}", extractPath(request), e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.FORBIDDEN.value())
                .message(Translator.toLocale("err.access_denied"))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 5. ResponseStatusException Handler
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException e, WebRequest request) {

        String reason = e.getReason() != null
                ? Translator.toLocale(e.getReason())
                : Translator.toLocale("err.uncategorized");

        log.warn("ResponseStatusException [{}]: {} at {}",
                e.getStatusCode(), reason, extractPath(request));

        ErrorResponse response = ErrorResponse.builder()
                .code(e.getStatusCode().value())
                .message(reason)
                .build();

        return ResponseEntity.status(e.getStatusCode()).body(response);
    }

    // 6. Uncategorized Exception - Bắt tất cả lỗi chưa được xử lý
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, WebRequest request) {
        log.error("Unhandled exception at {}: {}", extractPath(request), e.getMessage(), e);

        String message = Translator.toLocale("err.uncategorized");

        ErrorResponse response = ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(message)
                .build();

        return ResponseEntity.internalServerError().body(response);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract request path từ WebRequest
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * Extract constraint name từ DataIntegrityViolationException message
     */
    private String extractConstraintName(DataIntegrityViolationException e) {
        if (e.getMessage() == null) return "unknown";

        String message = e.getMessage();

        // Pattern 1: constraint [users.UKoyqqoj6pa43sdb3v8an8o7978]
        int start = message.indexOf("constraint [");
        if (start != -1) {
            int end = message.indexOf("]", start);
            if (end != -1) {
                return message.substring(start + 12, end);
            }
        }

        // Pattern 2: for key 'users.UKoyqqoj6pa43sdb3v8an8o7978'
        start = message.indexOf("for key '");
        if (start != -1) {
            int end = message.indexOf("'", start + 9);
            if (end != -1) {
                return message.substring(start + 9, end);
            }
        }

        return "unknown";
    }

    /**
     * Parse unique constraint error để trả message có nghĩa
     */
    private String parseUniqueConstraintError(String constraintName) {
        String lowerConstraint = constraintName.toLowerCase();

        // Map constraint names to meaningful messages
        if (lowerConstraint.contains("email")) {
            return Translator.toLocale("err.duplicate.email");
        } else if (lowerConstraint.contains("phone")) {
            return Translator.toLocale("err.duplicate.phone");
        } else if (lowerConstraint.contains("username")) {
            return Translator.toLocale("err.duplicate.username");
        }

        // Generic duplicate message
        return Translator.toLocale("err.duplicate_entry");
    }
}