package com.sbibank.openbanking.web;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Handles only genuinely cross-cutting, protocol-level failures that are identical across both flows:
 * malformed/missing request input never reaches either flow's business logic, so it can't be mapped into
 * either flow's own (mutually incompatible) response envelope. Business-logic errors are handled entirely
 * inside each flow's service - see {@code AccountInquiryService}/{@code FullFundTransferService} - which
 * never let exceptions escape, so this class should rarely be reached beyond validation failures.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> onValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return validationErrorResponse(details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> onConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return validationErrorResponse(details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> onMissingParam(MissingServletRequestParameterException ex) {
        return validationErrorResponse(List.of(ex.getParameterName() + ": required parameter is missing"));
    }

    /**
     * Spring MVC's native handler-method validation path (as opposed to the AOP-based
     * {@link ConstraintViolationException} path) for {@code @RequestParam}/{@code @PathVariable}
     * constraints, e.g. a present-but-blank {@code accountNumber}.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> onHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<String> details = ex.getAllErrors().stream()
                .map(error -> String.valueOf(error.getDefaultMessage()))
                .toList();
        return validationErrorResponse(details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> onMessageNotReadable(HttpMessageNotReadableException ex) {
        return validationErrorResponse(List.of("Malformed request body or invalid parameter format"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> onMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "method_not_allowed");
        body.put("details", List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> onMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "unsupported_media_type");
        body.put("details", List.of(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onUnhandled(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "internal_error");
        body.put("details", List.of("An unexpected error occurred"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> validationErrorResponse(List<String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        body.put("details", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
