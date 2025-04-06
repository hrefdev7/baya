**Core Features:**

User registration with email and password (hashed using BCrypt).
Login functionality with JWT (JSON Web Token) authentication.
A secure dashboard accessible only to authenticated users, displaying a welcome message with the user's name.

**Security:**
Use Spring Security to protect endpoints.
Implement role-based access control (e.g., USER and ADMIN roles).
Secure the dashboard endpoint to require authentication.
Add CSRF protection and proper CORS configuration.
Validate user input (e.g., email format, password strength) using Hibernate Validator.

**Database:**
Use Spring Data JPA with an H2 in-memory database for development and PostgreSQL support for production.
Create a User entity with fields: id, email, password, firstName, lastName, role, and createdAt (timestamp).
Include a database migration tool like Flyway or Liquibase for schema management.
