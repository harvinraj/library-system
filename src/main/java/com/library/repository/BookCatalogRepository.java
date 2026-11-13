package com.library.repository;

import com.library.model.BookCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookCatalogRepository extends JpaRepository<BookCatalog, String> {
}