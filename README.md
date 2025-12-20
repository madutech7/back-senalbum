# Senalbum Backend

This is the Spring Boot backend for the Senalbum application.

## Prerequisites

- Java 21 or higher
- Maven
- PostgreSQL

## Getting Started

1. Clone the repository.
2. Navigate to the `backend` directory.
3. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties`.
4. Fill in your configuration details (Database, Wasabi, PayDunya, Google OAuth).
5. Run the application:

```bash
mvn spring-boot:run
```

## Features

- REST API for gallery management
- Image upload and processing (local & Wasabi S3)
- Authentication (JWT & Google OAuth2)
- Subscription management with PayDunya
- PostgreSQL persistence with JPA/Hibernate
