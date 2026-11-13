# Library Management System (RESTful API)

A production-grade, concurrency-safe Library Management System built with **Java 17**, **Spring Boot 3**, **Hibernate/JPA**, and **PostgreSQL**, engineered specifically to prevent double-borrowing race conditions under high transactional load.

---

## Architecture & Database Design

- **Normalized Relational Schema:** Separates logical book metadata (`book_catalog`) from physical inventory units (`book_copy`), ensuring strict ISBN consistency (identical ISBNs must share the same title and author) while supporting multiple physical copies.
- **Concurrency Control:** Utilizes pessimistic locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) at the repository layer to serialize concurrent borrow attempts on the same book copy.
- **Standardized Error Handling:** Implements `@RestControllerAdvice` returning RFC 7807 Problem Details compliant error structures.
- **DTOs:** Leverages Java 17 **Records** for immutable, boilerplate-free data transfer objects.

---

## 🚀 Getting Started & Running with Docker

1. **Clone the repository and navigate to the root directory.**
2. **Start the application and PostgreSQL containers:**
   ```bash
   docker compose up --build -d