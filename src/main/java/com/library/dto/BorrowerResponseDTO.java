package com.library.dto;

public record BorrowerResponseDTO(
    Long id,
    String name,
    String email
) {}