package com.tickets.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank String body,
        @NotBlank String author
) {
}
