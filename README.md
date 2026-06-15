# GMC Retreat App

Backend foundation for **GMC Retreat App**, a church retreat preparation and operation system.

This repository is named `church-retreat-ops`.

## Phase 0 Scope

Phase 0 creates the local backend foundation only:

- Spring Boot backend project
- Java 21 Gradle build
- PostgreSQL local development database through Docker Compose
- Flyway migration setup
- MyBatis setup
- Spring Security shell configuration
- Common API response and error handling
- Basic health check API
- Testcontainers PostgreSQL context test

Business domains are intentionally not implemented yet. Participant registration, admin authentication, roles, groups, announcements, schedules, payments, QR check-in, audit logs, and privacy access logs belong to later phases.

## Tech Stack

- Java 21
- Spring Boot 3.5.15
- Gradle
- PostgreSQL
- MyBatis
- Flyway
- Spring Security
- JUnit 5
- Testcontainers PostgreSQL
- Lombok

Swagger/OpenAPI is not included in Phase 0 to keep local setup minimal. Security already allows the standard Swagger paths so OpenAPI can be added later without changing the public endpoint policy.

## Architecture Notes

- Participants will not create normal login accounts.
- Participant application and self-lookup data should live under future `registrations` tables.
- Staff login accounts should live under future `admin_users` tables for `STAFF`, `CHAIR`, `PASTOR`, and `SYSTEM_ADMIN`.
- The current security configuration permits only `/api/health` and reserves the structure for stateless JWT/admin authentication in Phase 1.
- The first Flyway migration creates only a small baseline table. No business tables are created in Phase 0.

## Local Database

Start PostgreSQL:

```bash
docker compose up -d
```

Database settings for the `local` profile:

- Host: `localhost`
- Port: `5432`
- Database: `church_retreat_ops`
- User: `retreat_app`
- Password: `retreat_app_password`

Stop PostgreSQL:

```bash
docker compose down
```

Remove the local database volume:

```bash
docker compose down -v
```

## Run Locally

Start PostgreSQL first, then run the application:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Expected response shape:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "checkedAt": "2026-06-15T00:00:00Z"
  },
  "timestamp": "2026-06-15T00:00:00Z"
}
```

## Tests

Run tests:

```bash
./gradlew test
```

The context test uses Testcontainers PostgreSQL, so Docker must be running.

## Project Structure

```text
src/main/java/com/gmc/retreat
  api/       common API response shape
  config/    framework configuration
  error/     common error codes and exception handling
  health/    health check endpoint
  security/  Spring Security configuration

src/main/resources
  db/migration/ Flyway migrations
  mapper/       MyBatis XML mapper location
```
