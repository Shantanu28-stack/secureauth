# Secure Authentication Service

A backend authentication and authorization service built with Java and Spring Boot. It handles user registration, login, and secure access control — including password hashing, JWT-based authentication, and protection against brute-force login attempts.

## Overview

This project provides a standalone REST API for user authentication that could sit behind any front-end or be integrated into a larger application. Rather than just checking a username/password pair, it's built the way a real production auth service would be — with proper validation, hashed passwords, token-based sessions, and account lockout after repeated failed logins.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.5.9
- **Security:** Spring Security
- **Database:** MySQL
- **ORM:** Spring Data JPA / Hibernate
- **Authentication:** JWT (JSON Web Tokens) via Nimbus JOSE+JWT
- **Build Tool:** Maven
- **Validation:** Jakarta Bean Validation

## Features

- User registration with input validation (valid email format, minimum password length, required fields)
- Duplicate email prevention
- Passwords hashed with BCrypt — never stored in plain text
- Login endpoint that verifies credentials and issues a signed JWT on success
- Role-based structure (`USER`, `ADMIN`) via a many-to-many relationship between users and roles
- Account lockout after repeated failed login attempts, to protect against brute-force attacks
- Custom JWT authentication filter to validate tokens on protected requests
- Centralized exception handling for clean, consistent JSON error responses

## Project Structure

```
src/main/java/com/shantanu/secureauth/
├── auth/            # Login request/response DTOs
├── config/          # Security beans, data initializer (default roles)
├── controller/       # REST controllers (AuthController)
├── dto/             # Register request/response DTOs
├── entity/          # JPA entities (User, Role)
├── exception/        # Custom exceptions + global exception handler
├── repository/       # Spring Data JPA repositories
├── security/         # JWT service and JWT authentication filter
└── service/          # Business logic (AuthService)
```

## Getting Started

### Prerequisites

- Java 17
- Maven
- MySQL Server running locally

### 1. Set up the database

```sql
CREATE DATABASE secureauth_db;
CREATE USER 'secureauth_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON secureauth_db.* TO 'secureauth_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configure the application

Update `src/main/resources/application.properties` with your own database credentials and JWT secret:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/secureauth_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=secureauth_user
spring.datasource.password=your_password

app.jwt.secret=ThisIsASecretKeyThatIsAtLeast32CharactersLong!!
app.jwt.expiration-ms=3600000
```

> **Note:** `spring.jpa.hibernate.ddl-auto` is set to `create`, which recreates all tables (and wipes existing data) on every restart — convenient for development, but should be changed to `update` or `validate` before any real/persistent use.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

## API Endpoints

### Register a new user

```
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Test User",
  "email": "test@example.com",
  "password": "password123"
}
```

**Response — 200 OK**
```json
{
  "message": "User registered successfully"
}
```

### Log in

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}
```

**Response — 200 OK**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0..."
}
```

**Response — 400 Bad Request** (wrong credentials, or account locked)
```json
{
  "error": "Invalid email or password"
}
```

## Security Notes

- Passwords are hashed with BCrypt before being stored — the raw password is never persisted.
- After 5 consecutive failed login attempts, the account is locked for 15 minutes.
- JWTs are signed using a secret key configured in `application.properties`, not hardcoded in source.

## Possible Future Improvements

- Add refresh tokens alongside access tokens
- Add email verification on registration
- Expose an admin-only endpoint to unlock accounts manually
- Move secrets out of `application.properties` and into environment variables for production use
