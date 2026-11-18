package com.tickets.web.dto;

import java.util.List;

public record ProblemResponse(
        int status,
        String title,
        String detail,
        List<FieldError> errors
) {

    public record FieldError(String field, String message) {
    }

    public static ProblemResponse of(int status, String title, String detail) {
        return new ProblemResponse(status, title, detail, null);
    }
}
