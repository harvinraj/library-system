package com.library.dto;

import java.time.LocalDateTime;

public record BookLoanResponseDTO(
    Long id,
    Long bookCopyId,
    String bookTitle,
    String borrowerName,
    LocalDateTime borrowedAt,
    LocalDateTime returnedAt
) {}