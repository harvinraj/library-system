package com.library.dto;

import jakarta.validation.constraints.NotNull;

public record BorrowRequestDTO(
    @NotNull(message = "Borrower ID is required")
    Long borrowerId,

    @NotNull(message = "Book ID (Copy ID) is required")
    Long bookCopyId
) {}