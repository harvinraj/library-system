package com.library.service;

import com.library.model.BookCatalog;
import com.library.model.BookCopy;
import com.library.model.Borrower;
import com.library.repository.BookCatalogRepository;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowerRepository;
import com.library.repository.BookLoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private BookCatalogRepository bookCatalogRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private BookLoanRepository loanRepository;

    private Long testBookCopyId;
    private Long borrower1Id;
    private Long borrower2Id;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookCatalogRepository.deleteAll();
        borrowerRepository.deleteAll();

        // 1. Setup Catalog and Book Copy
        BookCatalog catalog = bookCatalogRepository.save(new BookCatalog("978-0132350884", "Clean Code Concurrency", "Robert C. Martin"));
        BookCopy copy = bookCopyRepository.save(new BookCopy(catalog));
        testBookCopyId = copy.getId();

        // 2. Setup two distinct borrowers
        Borrower b1 = borrowerRepository.save(new Borrower("Alice Smith", "alice@example.com"));
        Borrower b2 = borrowerRepository.save(new Borrower("Bob Jones", "bob@example.com"));
        borrower1Id = b1.getId();
        borrower2Id = b2.getId();
    }

    @Test
    void testConcurrentBorrowingRaceCondition() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Thread 1 attempts to borrow the book for Alice
        executorService.submit(() -> {
            try {
                latch.await(); // Wait for signal to fire simultaneously
                libraryService.borrowBook(borrower1Id, testBookCopyId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        // Thread 2 attempts to borrow the exact same book copy for Bob at the same time
        executorService.submit(() -> {
            try {
                latch.await(); // Wait for signal to fire simultaneously
                libraryService.borrowBook(borrower2Id, testBookCopyId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        // Release the latch to trigger both threads simultaneously
        latch.countDown();
        executorService.shutdown();
        
        // Wait for threads to finish execution
        while (!executorService.isTerminated()) {
            Thread.sleep(50);
        }

        // Assertions: Exactly 1 thread must succeed, and exactly 1 must fail due to pessimistic locking / unavailability
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
    }
}