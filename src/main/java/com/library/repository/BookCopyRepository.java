package com.library.repository;

import com.library.model.BookCopy;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bc FROM BookCopy bc WHERE bc.id = :id")
    Optional<BookCopy> findByIdWithLock(@Param("id") Long id);
}