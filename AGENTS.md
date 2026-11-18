# Dealership project guidelines

## Requirements and checks

- Read [BRIEF.md](BRIEF.md) for the assignment, the fixed API contract, and delivery order. Read [INTENT.md](INTENT.md) for the product requirements, assumptions, and acceptance checks.
- Use JDK 21 and a supported Node.js version from [README.md](README.md). Use the Maven wrapper and the npm lockfile. Run `./test.sh` from the root. Run `npm run build` in `frontend/` when routes or templates change. Use `./run.sh` for a local smoke test.
- Use [feature-slice](.claude/skills/feature-slice/SKILL.md) when you add an entity across the backend and frontend. The vehicle module is the reference for conventions.

## Backend

- Put feature code under `backend/src/main/java/com/example/dealership/`, with matching test packages. Controllers use validated request records and return response records. Keep entity mapping in the mapper and write operations in the transactional service.
- Use the shared error types and `GlobalExceptionHandler`. Invalid requests use HTTP 400 with field errors. Missing resources use 404. Conflicting writes use 409. Follow `VehicleController` for successful responses.
- "Today" is the dealership date in `Europe/Warsaw`. Inject the `Clock` bean from `config/ClockConfig` instead of calling `LocalDate.now()` without a clock, and use a fixed clock in tests.
- Add schema changes as new Flyway migrations under `backend/src/main/resources/db/migration/`. Hibernate validates the schema. Check existing rows when adding constraints or defaults.
- Follow the existing MVC, service, and repository tests. Put database constraints and JPA mapping assertions in tests that run against the database.

## Frontend

- Put each feature under `frontend/src/app/`. Follow the vehicle module's standalone components, lazy routes, and separate HTML templates. Use `httpResource` for reads, `HttpClient` for writes, and Angular signal forms for create and edit.
- Use the route parameter and ProblemDetail helpers in `frontend/src/app/shared/`. Match request values to the backend, including nulls, enum values, and dates. Keep the existing loading, empty, and error states.
- Use the Angular test setup in `vehicle-form.spec.ts` for form and HTTP behavior. Assert actual request values and the user-visible result.
