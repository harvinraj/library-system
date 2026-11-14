package com.library.service;

import com.library.dto.BookCopyResponseDTO;
import com.library.dto.BorrowerResponseDTO;
import com.library.dto.BookLoanResponseDTO;
import com.library.exception.BookNotAvailableException;
import com.library.exception.InvalidIsbnException;
import com.library.exception.ResourceNotFoundException;
import com.library.model.*;
import com.library.repository.BookCatalogRepository;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowerRepository;
import com.library.repository.BookLoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    private final BookCatalogRepository bookCatalogRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowerRepository borrowerRepository;
    private final BookLoanRepository loanRepository;

    public LibraryService(BookCatalogRepository bookCatalogRepository,
                          BookCopyRepository bookCopyRepository,
                          BorrowerRepository borrowerRepository,
                          BookLoanRepository loanRepository) {
        this.bookCatalogRepository = bookCatalogRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowerRepository = borrowerRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public BorrowerResponseDTO registerBorrower(String name, String email) {
        log.info("Attempting to register borrower with email: {}", email);

        borrowerRepository.findByEmail(email).ifPresent(b -> {
            log.error("Failed to register borrower: Email {} already exists.", email);
            throw new InvalidIsbnException("Borrower with email " + email + " already exists.");
        });

        Borrower borrower = borrowerRepository.save(new Borrower(name, email));

        log.info("Borrower registered successfully with ID: {} and email: {}", borrower.getId(), borrower.getEmail());
        return new BorrowerResponseDTO(borrower.getId(), borrower.getName(), borrower.getEmail());
    }

    @Transactional
    public BookCopyResponseDTO registerBook(String isbn, String title, String author) {
        log.info("Attempting to register book copy for ISBN: {}", isbn);

        Optional<BookCatalog> existingCatalog = bookCatalogRepository.findById(isbn);

        BookCatalog catalog;
        if (existingCatalog.isPresent()) {
            catalog = existingCatalog.get();
            if (!catalog.getTitle().equalsIgnoreCase(title) || !catalog.getAuthor().equalsIgnoreCase(author)) {
                log.error("Failed to register book: ISBN {} exists with different title/author.", isbn);
                throw new InvalidIsbnException("ISBN " + isbn + " already exists with different title/author.");
            }
            log.info("Found existing book catalog for ISBN: {}. Adding new book copy.", isbn);
        } else {
            catalog = new BookCatalog(isbn, title, author);
            bookCatalogRepository.save(catalog);
            log.info("Created new book catalog for ISBN: {}", isbn);
        }

        BookCopy copy = bookCopyRepository.save(new BookCopy(catalog));
        log.info("Successfully registered book copy with ID: {} for ISBN: {}", copy.getId(), isbn);
        
        return new BookCopyResponseDTO(copy.getId(), catalog.getIsbn(), catalog.getTitle(), catalog.getAuthor(), copy.getStatus());
    }

    public List<BookCopyResponseDTO> getAllBooks() {
        log.info("Fetching all book copies from book catalog.");
        List<BookCopyResponseDTO> bookCopies = bookCopyRepository.findAll().stream()
                .map(copy -> new BookCopyResponseDTO(
                        copy.getId(),
                        copy.getBookCatalog().getIsbn(),
                        copy.getBookCatalog().getTitle(),
                        copy.getBookCatalog().getAuthor(),
                        copy.getStatus()
                ))
                .toList();
        log.info("Retrieved {} book copies.", bookCopies.size());
        return bookCopies;
    }

    @Transactional
    public BookLoanResponseDTO borrowBook(Long borrowerId, Long bookCopyId) {
        log.info("Attempting book loan transaction: Borrower ID {} requesting Book Copy ID {}", borrowerId, bookCopyId);

        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> {
                    log.error("Borrower not found with ID: {}", borrowerId);
                    return new ResourceNotFoundException("Borrower not found with ID: " + borrowerId);
                });

        BookCopy bookCopy = bookCopyRepository.findByIdWithLock(bookCopyId)
                .orElseThrow(() -> {
                    log.error("Book copy not found with ID: {}", bookCopyId);
                    return new ResourceNotFoundException("Book copy not found with ID: " + bookCopyId);
                });

        if (bookCopy.getStatus() != BookStatus.AVAILABLE) {
            log.error("Borrowing failed: Book copy ID {} is currently {}", bookCopyId, bookCopy.getStatus());
            throw new BookNotAvailableException("Book copy ID " + bookCopyId + " is currently borrowed or unavailable.");
        }

        bookCopy.setStatus(BookStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        BookLoan loan = new BookLoan();
        loan.setBookCopy(bookCopy);
        loan.setBorrower(borrower);
        loan.setBorrowedAt(LocalDateTime.now());
        BookLoan savedLoan = loanRepository.save(loan);

        log.info("Successfully completed loan ID {} for Borrower ID {} on Book Copy ID {}", savedLoan.getId(), borrowerId, bookCopyId);

        return new BookLoanResponseDTO(
                savedLoan.getId(),
                bookCopy.getId(),
                bookCopy.getBookCatalog().getTitle(),
                borrower.getName(),
                savedLoan.getBorrowedAt(),
                savedLoan.getReturnedAt()
        );
    }

    @Transactional
    public BookLoanResponseDTO returnBook(Long bookCopyId) {
        log.info("Attempting return transaction for Book Copy ID {}", bookCopyId);
        
        BookCopy bookCopy = bookCopyRepository.findByIdWithLock(bookCopyId)
                .orElseThrow(() -> {
                    log.error("Book copy not found for return with ID: {}", bookCopyId);
                    return new ResourceNotFoundException("Book copy not found with ID: " + bookCopyId);
                });

        if (bookCopy.getStatus() == BookStatus.AVAILABLE) {
            log.error("Return failed: Book copy ID {} is already marked as AVAILABLE.", bookCopyId);
            throw new BookNotAvailableException("Book copy ID " + bookCopyId + " is already marked as available.");
        }

        BookLoan activeLoan = loanRepository.findActiveLoanByBookCopyId(bookCopyId)
                .orElseThrow(() -> {
                    log.error("No active loan found for book copy ID: {}", bookCopyId);
                    return new ResourceNotFoundException("No active loan found for book copy ID: " + bookCopyId);
                });

        bookCopy.setStatus(BookStatus.AVAILABLE);
        bookCopyRepository.save(bookCopy);

        activeLoan.setReturnedAt(LocalDateTime.now());
        BookLoan savedLoan = loanRepository.save(activeLoan);

        log.info("Successfully completed book return for Book Copy ID {} on loan ID {}", bookCopyId, savedLoan.getId());

        return new BookLoanResponseDTO(
                savedLoan.getId(),
                bookCopy.getId(),
                bookCopy.getBookCatalog().getTitle(),
                activeLoan.getBorrower().getName(),
                savedLoan.getBorrowedAt(),
                savedLoan.getReturnedAt()
        );
    }
}