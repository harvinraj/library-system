package com.library.model;

import jakarta.persistence.*;

@Entity
@Table(name = "book_copy")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Corresponds to Book ID in assessment

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "isbn", nullable = false)
    private BookCatalog bookCatalog;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status = BookStatus.AVAILABLE;

    public BookCopy() {}

    public BookCopy(BookCatalog bookCatalog) {
        this.bookCatalog = bookCatalog;
        this.status = BookStatus.AVAILABLE;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BookCatalog getBookCatalog() { return bookCatalog; }
    public void setBookCatalog(BookCatalog bookCatalog) { this.bookCatalog = bookCatalog; }

    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }
}