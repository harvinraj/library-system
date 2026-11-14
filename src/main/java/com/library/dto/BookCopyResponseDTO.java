package com.library.dto;

import com.library.model.BookStatus;

public record BookCopyResponseDTO(
    Long id,
    String isbn,
    String title,
    String author,
    BookStatus status
) {}