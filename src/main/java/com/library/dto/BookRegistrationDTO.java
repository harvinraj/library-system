package com.library.dto;

import jakarta.validation.constraints.NotBlank;

public record BookRegistrationDTO(
    @NotBlank(message = "ISBN is required")
    String isbn,

    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Author is required")
    String author
) {}