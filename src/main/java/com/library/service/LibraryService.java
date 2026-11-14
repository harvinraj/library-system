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
        borrowerRepository.findByEmail(email).ifPresent(b -> {
            throw new InvalidIsbnException("Borrower with email " + email + " already exists.");
        });
        Borrower borrower = borrowerRepository.save(new Borrower(name, email));
        return new BorrowerResponseDTO(borrower.getId(), borrower.getName(), borrower.getEmail());
    }

    @Transactional
    public BookCopyResponseDTO registerBook(String isbn, String title, String author) {
        Optional<BookCatalog> existingCatalog = bookCatalogRepository.findById(isbn);

        BookCatalog catalog;
        if (existingCatalog.isPresent()) {
            catalog = existingCatalog.get();
            if (!catalog.getTitle().equalsIgnoreCase(title) || !catalog.getAuthor().equalsIgnoreCase(author)) {
                throw new InvalidIsbnException("ISBN " + isbn + " already exists with different title/author.");
            }
        } else {
            catalog = new BookCatalog(isbn, title, author);
            bookCatalogRepository.save(catalog);
        }

        BookCopy copy = bookCopyRepository.save(new BookCopy(catalog));
        return new BookCopyResponseDTO(copy.getId(), catalog.getIsbn(), catalog.getTitle(), catalog.getAuthor(), copy.getStatus());
    }

    public List<BookCopyResponseDTO> getAllBooks() {
        return bookCopyRepository.findAll().stream()
                .map(copy -> new BookCopyResponseDTO(
                        copy.getId(),
                        copy.getBookCatalog().getIsbn(),
                        copy.getBookCatalog().getTitle(),
                        copy.getBookCatalog().getAuthor(),
                        copy.getStatus()
                ))
                .toList();
    }

    @Transactional
    public BookLoanResponseDTO borrowBook(Long borrowerId, Long bookCopyId) {
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
        BookLoan savedLoan = loanRepository.save(loan);

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
        BookCopy bookCopy = bookCopyRepository.findByIdWithLock(bookCopyId)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with ID: " + bookCopyId));

        if (bookCopy.getStatus() == BookStatus.AVAILABLE) {
            throw new BookNotAvailableException("Book copy ID " + bookCopyId + " is already marked as available.");
        }

        BookLoan activeLoan = loanRepository.findActiveLoanByBookCopyId(bookCopyId)
                .orElseThrow(() -> new ResourceNotFoundException("No active loan found for book copy ID: " + bookCopyId));

        bookCopy.setStatus(BookStatus.AVAILABLE);
        bookCopyRepository.save(bookCopy);

        activeLoan.setReturnedAt(LocalDateTime.now());
        BookLoan savedLoan = loanRepository.save(activeLoan);

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