package com.library.controller;

import com.library.dto.BookRegistrationDTO;
import com.library.dto.BorrowRequestDTO;
import com.library.dto.BorrowerRegistrationDTO;
import com.library.model.BookCopy;
import com.library.model.Borrower;
import com.library.model.BookLoan;
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

    // 1. Register a new borrower
    @PostMapping("/borrower")
    public ResponseEntity<Borrower> registerBorrower(@Valid @RequestBody BorrowerRegistrationDTO request) {
        Borrower borrower = libraryService.registerBorrower(request.name(), request.email());
        return new ResponseEntity<>(borrower, HttpStatus.CREATED);
    }

    // 2. Register a new book
    @PostMapping("/book")
    public ResponseEntity<BookCopy> registerBook(@Valid @RequestBody BookRegistrationDTO request) {
        BookCopy bookCopy = libraryService.registerBook(request.isbn(), request.title(), request.author());
        return new ResponseEntity<>(bookCopy, HttpStatus.CREATED);
    }

    // 3. Get a list of all books
    @GetMapping("/books")
    public ResponseEntity<List<BookCopy>> getAllBooks() {
        List<BookCopy> books = libraryService.getAllBooks();
        return ResponseEntity.ok(books);
    }

    // 4. Borrow a book on behalf of a borrower
    @PostMapping("/loan/borrow")
    public ResponseEntity<BookLoan> borrowBook(@Valid @RequestBody BorrowRequestDTO request) {
        BookLoan bookLoan = libraryService.borrowBook(request.borrowerId(), request.bookCopyId());
        return new ResponseEntity<>(bookLoan, HttpStatus.CREATED);
    }

    // 5. Return a borrowed book
    @PostMapping("/loan/return/{bookCopyId}")
    public ResponseEntity<BookLoan> returnBook(@PathVariable Long bookCopyId) {
        BookLoan bookLoan = libraryService.returnBook(bookCopyId);
        return ResponseEntity.ok(bookLoan);
    }
}