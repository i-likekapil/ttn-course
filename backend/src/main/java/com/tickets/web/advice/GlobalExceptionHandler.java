package com.tickets.web.advice;

import com.tickets.exception.InvalidQueryParameterException;
import com.tickets.exception.InvalidRequestFieldException;
import com.tickets.exception.InvalidStatusTransitionException;
import com.tickets.exception.TicketNotFoundException;
import com.tickets.web.dto.ProblemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ProblemResponse> handleTicketNotFound(TicketNotFoundException ex) {
        ProblemResponse body = ProblemResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not found",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ProblemResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        ProblemResponse body = ProblemResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidRequestFieldException.class)
    public ResponseEntity<ProblemResponse> handleInvalidRequestField(InvalidRequestFieldException ex) {
        ProblemResponse body = new ProblemResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "One or more fields are invalid",
                List.of(new ProblemResponse.FieldError(ex.getField(), ex.getMessage())));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidQueryParameterException.class)
    public ResponseEntity<ProblemResponse> handleInvalidQueryParameter(InvalidQueryParameterException ex) {
        ProblemResponse body = new ProblemResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "One or more fields are invalid",
                List.of(new ProblemResponse.FieldError(ex.getParameter(), ex.getMessage())));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ProblemResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        ProblemResponse body = new ProblemResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                "One or more fields are invalid",
                errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof InvalidRequestFieldException invalidField) {
                return handleInvalidRequestField(invalidField);
            }
            cause = cause.getCause();
        }
        String detail = "Malformed JSON or invalid request body";
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException invalidFormat) {
            String field = invalidFormat.getPath().isEmpty()
                    ? "body"
                    : invalidFormat.getPath().getLast().getFieldName();
            detail = "Invalid value for field: " + field;
            ProblemResponse body = new ProblemResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Validation failed",
                    "One or more fields are invalid",
                    List.of(new ProblemResponse.FieldError(field, detail)));
            return ResponseEntity.badRequest().body(body);
        }
        ProblemResponse body = ProblemResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                detail);
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemResponse.FieldError toFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "is invalid";
        return new ProblemResponse.FieldError(fieldError.getField(), message);
    }
}
