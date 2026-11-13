package com.library.service;

import com.library.exception.BookNotAvailableException;
import com.library.exception.InvalidIsbnException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.*;
import com.library.repository.BookCatalogRepository;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowerRepository;
import com.library.repository.BookLoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LibraryService {

    private final BookCatalogRepository bookCatalogRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowerRepository borrowerRepository;
    private final BookLoanRepository bookLoanRepository;

    public LibraryService(BookCatalogRepository bookCatalogRepository,
                          BookCopyRepository bookCopyRepository,
                          BorrowerRepository borrowerRepository,
                          BookLoanRepository bookLoanRepository) {
        this.bookCatalogRepository = bookCatalogRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowerRepository = borrowerRepository;
        this.bookLoanRepository = bookLoanRepository;
    }

    // 1. Register a new borrower
    @Transactional
    public Borrower registerBorrower(String name, String email) {
        borrowerRepository.findByEmail(email).ifPresent(b -> {
            throw new InvalidIsbnException("Borrower with email " + email + " already exists.");
        });
        Borrower borrower = new Borrower(name, email);
        return borrowerRepository.save(borrower);
    }

    // 2. Register a new book (or copy) adhering to ISBN rule
    @Transactional
    public BookCopy registerBook(String isbn, String title, String author) {
        Optional<BookCatalog> existingCatalog = bookCatalogRepository.findById(isbn);

        BookCatalog catalog;
        if (existingCatalog.isPresent()) {
            catalog = existingCatalog.get();
            // Rule: 2 books with the same ISBN numbers must have the same title and author
            if (!catalog.getTitle().equalsIgnoreCase(title) || !catalog.getAuthor().equalsIgnoreCase(author)) {
                throw new InvalidIsbnException("ISBN " + isbn + " already exists with different title/author.");
            }
        } else {
            catalog = new BookCatalog(isbn, title, author);
            bookCatalogRepository.save(catalog);
        }

        // Create and save a new physical copy (Multiple copies with same ISBN are allowed)
        BookCopy copy = new BookCopy(catalog);
        return bookCopyRepository.save(copy);
    }

    // 3. Get a list of all books (catalogs with their copies and statuses)
    public List<BookCopy> getAllBooks() {
        return bookCopyRepository.findAll();
    }

    // 4. Borrow a book with pessimistic locking to prevent race conditions
    @Transactional
    public BookLoan borrowBook(Long borrowerId, Long bookCopyId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower not found with ID: " + borrowerId));

        BookCopy bookCopy = bookCopyRepository.findByIdWithLock(bookCopyId)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with ID: " + bookCopyId));

        if (bookCopy.getStatus() != BookStatus.AVAILABLE) {
            throw new BookNotAvailableException("Book copy ID " + bookCopyId + " is currently borrowed or unavailable.");
        }

        bookCopy.setStatus(BookStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        BookLoan loan = new BookLoan();
        loan.setBookCopy(bookCopy);
        loan.setBorrower(borrower);
        loan.setBorrowedAt(LocalDateTime.now());

        return bookLoanRepository.save(loan);
    }

    // 5. Return a borrowed book
    @Transactional
    public BookLoan returnBook(Long bookCopyId) {
        BookCopy bookCopy = bookCopyRepository.findByIdWithLock(bookCopyId)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with ID: " + bookCopyId));

        if (bookCopy.getStatus() == BookStatus.AVAILABLE) {
            throw new BookNotAvailableException("Book copy ID " + bookCopyId + " is already marked as available.");
        }

        BookLoan activeLoan = bookLoanRepository.findActiveLoanByBookCopyId(bookCopyId)
                .orElseThrow(() -> new ResourceNotFoundException("No active loan found for book copy ID: " + bookCopyId));

        bookCopy.setStatus(BookStatus.AVAILABLE);
        bookCopyRepository.save(bookCopy);

        activeLoan.setReturnedAt(LocalDateTime.now());
        return bookLoanRepository.save(activeLoan);
    }
}