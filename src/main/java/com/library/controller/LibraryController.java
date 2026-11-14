package com.library.controller;

import com.library.dto.*;
import com.library.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping("/borrowers")
    public ResponseEntity<BorrowerResponseDTO> registerBorrower(@Valid @RequestBody BorrowerRegistrationDTO request) {
        BorrowerResponseDTO response = libraryService.registerBorrower(request.name(), request.email());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping ("/borrowers")
    public ResponseEntity<List<BorrowerResponseDTO>> getAllBorrowers() {
        List<BorrowerResponseDTO> borrowers = libraryService.getAllBorrowers();
        return ResponseEntity.ok(borrowers);
    }

    @PostMapping("/books")
    public ResponseEntity<BookCopyResponseDTO> registerBook(@Valid @RequestBody BookRegistrationDTO request) {
        BookCopyResponseDTO response = libraryService.registerBook(request.isbn(), request.title(), request.author());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookCopyResponseDTO>> getAllBooks() {
        List<BookCopyResponseDTO> books = libraryService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    @PostMapping("/loans/borrow")
    public ResponseEntity<BookLoanResponseDTO> borrowBook(@Valid @RequestBody BorrowRequestDTO request) {
        BookLoanResponseDTO response = libraryService.borrowBook(request.borrowerId(), request.bookCopyId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/loans/return/{bookCopyId}")
    public ResponseEntity<BookLoanResponseDTO> returnBook(@PathVariable Long bookCopyId) {
        BookLoanResponseDTO response = libraryService.returnBook(bookCopyId);
        return ResponseEntity.ok(response);
    }
}