# AGENTS.md

## Project

This repository is `church-retreat-ops`, a Spring Boot backend for GMC Retreat App.

The app manages church retreat operations such as:

* admin authentication and roles
* participant registration
* participant self lookup/update
* admin participant management
* future community structure, group assignment, announcements, schedules, and check-in

## Current Stack

* Java 21
* Spring Boot
* PostgreSQL
* Flyway
* MyBatis
* Gradle
* JWT authentication
* BCrypt password/hash handling

## Work Rules

* Do not push to GitHub. The user handles commits and pushes unless explicitly requested otherwise.
* Before making broad changes, inspect relevant files and summarize the plan.
* Keep changes limited to the requested phase or task.
* Do not implement future phases unless explicitly requested.
* Reuse existing package structure, error handling, response format, security style, and test style.
* Add or update tests for behavior changes.
* Update README or docs when adding a new phase-level feature.

## Git Rules

* Do not work directly on `main`.
* Use `feature/*` for feature work.
* Use `docs/*` for documentation-only work.
* Use `chore/*` for repository maintenance.
* If the current branch is `main`, stop and ask before modifying files.

## Flyway Migration Rules

* Do not modify existing Flyway migration files after they may have been applied.
* For schema changes, always create a new migration file with the next version.
* Never use `flyway repair` unless explicitly instructed.
* Local development DB may be reset with `docker compose down -v` only when test data preservation is not required.

## Security and Privacy Rules

* Participants are not admin users.
* Do not store participant lookup keys in plaintext.
* Never expose `lookupKeyHash` in API responses, logs, docs examples, or histories.
* Keep participant APIs and admin APIs clearly separated.
* Admin APIs must require JWT authentication.
* Sensitive participant detail views must be protected by server-side authorization.
* When participant phone numbers or sensitive details are viewed by admins, preserve privacy access logging behavior.
* Do not rely on frontend-only controls as real security.

## Role Rules

Role hierarchy:

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

* STAFF can perform operational reads where allowed.
* CHAIR and PASTOR can perform higher-level retreat operation changes.
* SYSTEM_ADMIN is reserved for system-level override and maintenance.
* Newcomer/care target is a participant attribute, not an admin role.
* Community affiliation such as middle group/cell must remain separate from admin role.

## Test Commands

Use this command before reporting completion:

```bash
./gradlew clean test
```

If Docker, Testcontainers, or local PostgreSQL is required, mention that clearly when tests cannot run.
