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

## Token Efficiency and Work Quality Rules

Please use tokens efficiently, but do not sacrifice important implementation details.

Guidelines:

* Use Serena and targeted code search first before opening large files.
* Avoid reading or printing entire files unless necessary.
* Inspect only the files directly related to the current task.
* Before coding, summarize the minimal implementation plan.
* Reuse existing project patterns instead of inventing new structures.
* Keep changes scoped to the requested phase or task.
* Do not implement future-phase features unless explicitly requested.
* Prefer small, focused changes over broad rewrites.
* Avoid unnecessary explanations, long diffs, or repeated summaries.
* When reporting results, summarize changed files and important decisions instead of dumping full code.

However, do not over-optimize for token savings in a way that reduces correctness.

Do not skip:

* domain modeling decisions
* authorization checks
* privacy/security rules
* Flyway migration consistency
* DTO/API response safety
* validation and business rules
* tests
* README/docs/http updates for phase-level features
* checking that `lookupKeyHash` or `lookup_key_hash` is not externally exposed

If a file or flow is important for correctness, inspect it even if it costs more tokens.

The priority order is:

1. Correctness
2. Security and privacy
3. Consistency with existing project style
4. Test coverage
5. Documentation accuracy
6. Token efficiency

MCP usage rules:
- Use PostgreSQL MCP only for read-only schema inspection and SELECT verification.
- Do not use DB MCP to mutate data.
- Use GitHub MCP for repository, PR, Actions, and status inspection only.
- Do not create, close, merge, or modify GitHub PRs/issues without explicit user approval.
- Do not push commits or modify remote branches.
- Never expose or log secrets, PATs, database URLs, lookup keys, lookupKeyHash, lookup_key_hash, tokenHash, token_hash, or QR token hashes.

# Codex Work Guidelines

> Prioritize caution over speed. Judge minor tasks according to context.

## 1. Think before implementing — Do not assume. Do not hide ambiguity.

- Ask questions when something is uncertain. If multiple interpretations are possible, present the alternatives.
- Suggest a simpler approach if one exists.
- If any part is unclear, stop the task and ask a specific question.

## 2. Prefer simplicity — Write only the minimum necessary code.

- Do not add unrequested features, abstractions, or flexibility.
- Do not add exception handling for scenarios that cannot realistically occur.
- Ask: "Would this look overly complex to a senior engineer?" If yes, simplify it.

## 3. Make precise changes — Modify only what is necessary.

- Do not arbitrarily improve adjacent code, comments, or formatting.
- Do not refactor code that is not broken.
- If dead code is found, report it, but do not delete it directly.
- Every changed line must be directly connected to the user's request.

## 4. Execute with the goal in mind — Define the success criteria.

- "Bug fix" → "Write a test that reproduces the bug, then confirm it passes."
- For multi-step tasks, first create a plan in the form of `[step → verification]`.