package com.library.config;

import com.library.model.BookCatalog;
import com.library.model.BookCopy;
import com.library.model.Borrower;
import com.library.repository.BookCatalogRepository;
import com.library.repository.BookCopyRepository;
import com.library.repository.BorrowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DatabaseSeeder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    @Bean
    @Order(1) // Ensure this runs before other CommandLineRunners
    CommandLineRunner initDatabase(BorrowerRepository borrowerRepository,
                                  BookCatalogRepository bookCatalogRepository,
                                  BookCopyRepository bookCopyRepository) {
        return args -> {

            if (borrowerRepository.count() > 0) {
                log.info("Database already seeded. Skipping initialization.");
                return;
            }
            
            log.info("Initializing database seeding...");

            // 1. Seed 2 Borrowers
            Borrower borrower1 = borrowerRepository.save(new Borrower("Alice Smith", "alice@library.com"));
            Borrower borrower2 = borrowerRepository.save(new Borrower("Bob Jones", "bob@library.com"));
            log.info("Seeded 2 borrowers: {}, {}", borrower1.getName(), borrower2.getName());

            // 2. Seed 2 Book Catalogs & 3 Copies Each
            BookCatalog catalog1 = bookCatalogRepository.save(new BookCatalog("978-0132350884", "Clean Code", "Robert C. Martin"));
            BookCatalog catalog2 = bookCatalogRepository.save(new BookCatalog("978-0201633610", "Design Patterns", "Erich Gamma"));

            for (int i = 1; i <= 3; i++) {
                bookCopyRepository.save(new BookCopy(catalog1));
                bookCopyRepository.save(new BookCopy(catalog2));
            }
            log.info("Seeded 2 book catalogs ('{}' and '{}') with 3 physical copies each.", catalog1.getTitle(), catalog2.getTitle());
            log.info("Database seeding completed successfully.");
        };
    }
}