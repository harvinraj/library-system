package com.library.service;

import com.library.exception.BookNotAvailableException;
import com.library.exception.InvalidIsbnException;
import com.library.model.*;
import com.library.repository.BookCatalogRepository;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowerRepository;
import com.library.repository.BookLoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private BookCatalogRepository bookCatalogRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BookLoanRepository loanRepository;

    @InjectMocks
    private LibraryService libraryService;

    private Borrower sampleBorrower;
    private BookCatalog sampleCatalog;
    private BookCopy sampleCopy;

    @BeforeEach
    void setUp() {
        sampleBorrower = new Borrower("Alice Smith", "alice@example.com");
        sampleBorrower.setId(1L);

        sampleCatalog = new BookCatalog("978-0132350884", "Clean Code", "Robert C. Martin");
        
        sampleCopy = new BookCopy(sampleCatalog);
        sampleCopy.setId(101L);
        sampleCopy.setStatus(BookStatus.AVAILABLE);
    }

    @Test
    void registerBorrower_Success() {
        when(borrowerRepository.findByEmail(sampleBorrower.getEmail())).thenReturn(Optional.empty());
        when(borrowerRepository.save(any(Borrower.class))).thenReturn(sampleBorrower);

        Borrower result = libraryService.registerBorrower("Alice Smith", "alice@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(borrowerRepository, times(1)).save(any(Borrower.class));
    }

    @Test
    void registerBorrower_DuplicateEmail_ThrowsException() {
        when(borrowerRepository.findByEmail(sampleBorrower.getEmail())).thenReturn(Optional.of(sampleBorrower));

        assertThatThrownBy(() -> libraryService.registerBorrower("Alice Smith", "alice@example.com"))
                .isInstanceOf(InvalidIsbnException.class)
                .hasMessageContaining("already exists");

        verify(borrowerRepository, never()).save(any(Borrower.class));
    }

    @Test
    void registerBook_NewCatalogAndCopy_Success() {
        when(bookCatalogRepository.findById(sampleCatalog.getIsbn())).thenReturn(Optional.empty());
        when(bookCatalogRepository.save(any(BookCatalog.class))).thenReturn(sampleCatalog);
        when(bookCopyRepository.save(any(BookCopy.class))).thenReturn(sampleCopy);

        BookCopy result = libraryService.registerBook("978-0132350884", "Clean Code", "Robert C. Martin");

        assertThat(result).isNotNull();
        verify(bookCatalogRepository, times(1)).save(any(BookCatalog.class));
        verify(bookCopyRepository, times(1)).save(any(BookCopy.class));
    }

    @Test
    void registerBook_ExistingIsbnDifferentTitle_ThrowsException() {
        when(bookCatalogRepository.findById(sampleCatalog.getIsbn())).thenReturn(Optional.of(sampleCatalog));

        // Attempting to register same ISBN with a different title
        assertThatThrownBy(() -> libraryService.registerBook("978-0132350884", "Different Title", "Robert C. Martin"))
                .isInstanceOf(InvalidIsbnException.class)
                .hasMessageContaining("already exists with different title/author");

        verify(bookCopyRepository, never()).save(any(BookCopy.class));
    }

    @Test
    void borrowBook_Success() {
        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(sampleBorrower));
        when(bookCopyRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCopy));
        when(loanRepository.save(any(BookLoan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookLoan loan = libraryService.borrowBook(1L, 101L);

        assertThat(loan).isNotNull();
        assertThat(loan.getBookCopy().getStatus()).isEqualTo(BookStatus.BORROWED);
        verify(bookCopyRepository, times(1)).save(sampleCopy);
        verify(loanRepository, times(1)).save(any(BookLoan.class));
    }

    @Test
    void borrowBook_AlreadyBorrowed_ThrowsException() {
        sampleCopy.setStatus(BookStatus.BORROWED);

        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(sampleBorrower));
        when(bookCopyRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sampleCopy));

        assertThatThrownBy(() -> libraryService.borrowBook(1L, 101L))
                .isInstanceOf(BookNotAvailableException.class)
                .hasMessageContaining("currently borrowed or unavailable");

        verify(bookCopyRepository, never()).save(any(BookCopy.class));
        verify(loanRepository, never()).save(any(BookLoan.class));
    }
}