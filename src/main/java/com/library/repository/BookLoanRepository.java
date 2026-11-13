package com.library.repository;

import com.library.model.BookLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    @Query("SELECT bl FROM BookLoan bl WHERE bl.bookCopy.id = :bookCopyId AND bl.returnedAt IS NULL")
    Optional<BookLoan> findActiveLoanByBookCopyId(@Param("bookCopyId") Long bookCopyId);
}