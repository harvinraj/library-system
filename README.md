# Library Management System API

A production-grade, concurrency-safe RESTful API for managing a library system, built with **Java 17**, **Spring Boot 3**, **PostgreSQL**, and **Docker**.

---

## Table of Contents
1. [Architecture & Design Decisions](#architecture--design-decisions)
2. [Database Choice Justification](#database-choice-justification)
3. [Project Assumptions](#project-assumptions)
4. [12-Factor App & Clean Code Conformance](#12-factor-app--clean-code-conformance)
5. [Getting Started & Containerization](#getting-started--containerization)
6. [Database Seeding](#database-seeding)
7. [API Documentation & Usage Examples](#api-documentation--usage-examples)
8. [Testing & Concurrency Verification](#testing--concurrency-verification)

---

## Architecture & Design Decisions

### 1. Book Catalog vs. Physical Book Copy Separation
To satisfy the requirement that multiple physical copies of the same book share an ISBN while maintaining unique IDs and independent borrowing states, the domain model separates metadata from inventory:
* **`BookCatalog` (ISBN Primary Key):** Stores the unique `isbn`, `title`, and `author`. Enforces strict consistency so two books with the same ISBN cannot be registered with different titles or authors.
* **`BookCopy` (Auto-generated Long ID):** Represents a physical copy linked to a `BookCatalog`. Each copy tracks its own `status` (`AVAILABLE` or `BORROWED`).

### 2. Concurrency Control (`PESSIMISTIC_WRITE`)
To guarantee that **no more than one borrower can borrow the same book copy simultaneously** under high concurrency:
* The borrowing workflow acquires a database-level pessimistic write lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)` translating to SQL `SELECT ... FOR UPDATE`) on the target `BookCopy` row inside an ACID transaction (`@Transactional`).
* Concurrent requests targeting the exact same book copy ID are serialized at the database engine level, eliminating race conditions and double-borrowing.

### 3. Global Exception Handling (RFC 7807)
All exceptions are intercepted centrally via `@RestControllerAdvice` and serialized into standardized **RFC 7807 `ProblemDetail`** JSON payloads containing machine-readable `errorCode` attributes, HTTP status codes, field-level validation breakdowns, and timestamps.

---

## Database Choice Justification

**PostgreSQL** was chosen as the primary relational database for the following reasons:
1. **Strict ACID Compliance:** Essential for transactional integrity when updating book availability status and creating loan records atomically.
2. **Row-Level Locking (`FOR UPDATE`):** PostgreSQL provides mature, low-overhead row-level pessimistic locking (`PESSIMISTIC_WRITE`) without locking entire tables, ensuring high throughput while preventing double-borrowing race conditions.
3. **Relational & Referential Integrity:** Enforces foreign key constraints between `borrowers`, `book_catalogs`, `book_copies`, and `book_loans`.

---

## Project Assumptions

1. **ISBN Immutability & Normalization:** Once an ISBN is registered with a specific `title` and `author`, any subsequent registration using that same ISBN must match the existing title and author (case-insensitive). Attempting to register the same ISBN with conflicting metadata throws a `400 Bad Request` (`InvalidException`).
2. **Same Title/Author with Different ISBNs:** Treated as completely distinct book editions in `BookCatalog`, per specification.
3. **Unique Borrower Emails:** Each borrower must register with a unique email address. Duplicate email registrations are rejected with a `400 Bad Request`.
4. **Loan Lifecycle:** A borrower can borrow multiple different book copies, provided each copy is currently `AVAILABLE`. Returning a book copy marks the active `BookLoan` record with a `returnedAt` timestamp and restores the `BookCopy` status to `AVAILABLE`.
5. **Stateless API:** Authentication/authorization layers were omitted as the prompt specifies an API user acting on behalf of borrowers via REST endpoints.

---

## 12-Factor App & Clean Code Conformance

* **Codebase & Dependencies:** Managed declaratively via standard build tools (`pom.xml` / `build.gradle`) with isolated dependencies.
* **Configuration (`application.yaml`):** Hierarchical YAML configuration supporting environment variable overrides (`SPRING_DATASOURCE_URL`, etc.) for seamless deployment across local, staging, and production environments.
* **Stateless Processes:** The Spring Boot service holds zero in-memory session state; all state is persisted in PostgreSQL, allowing horizontal scaling.
* **Logs as Event Streams:** Uses SLF4J/Logback structured logging (`INFO` for transaction milestones, `ERROR` for business rule violations and exceptions) directed to `stdout`.
* **Clean Code Practices:** Utilizes **Java 17 Immutable Records** for Request/Response DTOs to prevent Hibernate lazy-loading proxy serialization issues and maintain clean separation between persistence entities and API contracts.

---

## Getting Started & Containerization

### Prerequisites
* Docker & Docker Compose
* Java 17+ (if running locally outside Docker)

### Run with Docker Compose (Recommended)
The entire stack (PostgreSQL database + Spring Boot API) is containerized using a multi-stage `Dockerfile` and orchestrated via `docker-compose.yml`:

```bash
# Build and start the database and application containers
docker compose up --build -d

# View application logs
docker compose logs -f app
```

The API will be available at `http://localhost:8080`.

---

## Database Seeding

On startup, `DatabaseSeeder` automatically populates the database (if empty) with initial test data so you can test endpoints immediately:
* **2 Borrowers:**
  * ID `1`: `Alice Smith` (`alice@library.com`)
  * ID `2`: `Bob Jones` (`bob@library.com`)
* **2 Book Catalogs with 3 Physical Copies Each (6 copies total):**
  * ISBN `978-0132350884` — *Clean Code* by Robert C. Martin (Copy IDs `1`, `2`, `3`)
  * ISBN `978-0201633610` — *Design Patterns* by Erich Gamma (Copy IDs `4`, `5`, `6`)


> **⚠️ Important Note on Database Persistence & Clean Restarts:**
> This project uses a named Docker volume (`postgres_data`) to persist PostgreSQL records across container restarts. 
> 
> While the `DatabaseSeeder` is idempotent (safely skipping initialization if records already exist), if you want to wipe existing test transactions and reset the database back to its clean initial seeded state (2 borrowers, 2 book catalogs, and 6 physical copies), stop the containers and remove the persistent volume before restarting:
> ```bash
> docker compose down -v
> docker compose up --build
> ```

---

## API Documentation & Usage Examples

### 1. Register a New Borrower
* **Endpoint:** `POST /api/v1/library/borrowers`
* **cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/library/borrowers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Charlie Brown",
    "email": "charlie@library.com"
  }'
```
* **Response (`201 Created`):**
```json
{
  "id": 3,
  "name": "Charlie Brown",
  "email": "charlie@library.com"
}
```

---

### 2. Register a New Book (or Add a Copy to Existing ISBN)
* **Endpoint:** `POST /api/v1/library/books`
* **cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/library/books \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0134685991",
    "title": "Effective Java",
    "author": "Joshua Bloch"
  }'
```
* **Response (`201 Created`):**
```json
{
  "id": 7,
  "isbn": "978-0134685991",
  "title": "Effective Java",
  "author": "Joshua Bloch",
  "status": "AVAILABLE"
}
```

---

### 3. Get a List of All Books in the Library
* **Endpoint:** `GET /api/v1/library/books`
* **cURL Example:**
```bash
curl -X GET http://localhost:8080/api/v1/library/books
```
* **Response (`200 OK`):**
```json
[
  {
    "id": 1,
    "isbn": "978-0132350884",
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "status": "AVAILABLE"
  }
]
```

---

### 4. Borrow a Book Copy
* **Endpoint:** `POST /api/v1/library/loans/borrow`
* **cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/library/loans/borrow \
  -H "Content-Type: application/json" \
  -d '{
    "borrowerId": 1,
    "bookCopyId": 1
  }'
```
* **Response (`200 OK`):**
```json
{
  "loanId": 1,
  "bookCopyId": 1,
  "bookTitle": "Clean Code",
  "borrowerName": "Alice Smith",
  "borrowedAt": "2026-11-14T16:30:00.123456",
  "returnedAt": null
}
```

---

### 5. Return a Borrowed Book Copy
* **Endpoint:** `POST /api/v1/library/loans/return`
* **cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/library/loans/return \
  -H "Content-Type: application/json" \
  -d '{
    "bookCopyId": 1
  }'
```
* **Response (`200 OK`):**
```json
{
  "loanId": 1,
  "bookCopyId": 1,
  "bookTitle": "Clean Code",
  "borrowerName": "Alice Smith",
  "borrowedAt": "2026-11-14T16:30:00.123456",
  "returnedAt": "2026-11-14T16:45:12.654321"
}
```

---

### 6. Get All Borrowers
* **Endpoint:** `GET /api/v1/library/borrowers`
* **cURL Example:**
```bash
curl -X GET http://localhost:8080/api/v1/library/borrowers
```
* **Response (`200 OK`):**
```json
[
  {
    "id": 1,
    "name": "Alice Smith",
    "email": "alice@library.com"
  },
  {
    "id": 2,
    "name": "Bob Jones",
    "email": "bob@library.com"
  }
]
```

---

### Standardized Error Response Example (RFC 7807)
When validation fails or a book copy is already borrowed:
```json
{
  "type": "https://api.library.com/errors/business-rule-violation",
  "title": "Business Rule Violation",
  "status": 400,
  "detail": "Book copy ID 1 is currently borrowed or unavailable.",
  "instance": "/api/v1/library/loans/borrow",
  "errorCode": "BUSINESS_RULE_VIOLATION",
  "timestamp": "2026-11-14T16:35:10.112233"
}
```

---

## Testing & Concurrency Verification

Run the automated test suite using Maven/Gradle:
```bash
./mvnw test
# or
./gradlew test
```
* **Unit Tests (`LibraryServiceTest`):** Verifies all business rules, ISBN conflict checks, duplicate email guards, and exception throwing using JUnit 5, Mockito, and AssertJ.
* **Concurrency Integration Tests:** Uses `ExecutorService` and `CountDownLatch` to simulate multiple simultaneous threads attempting to borrow the exact same `BookCopy` ID against PostgreSQL, confirming that only **1 thread succeeds** while all competing threads receive `BookNotAvailableException`.