---
name: feature-slice
description: Implement a dealership entity from a description as a Spring Boot and Angular feature with persistence, API, UI, and tests. Use for a complete CRUD slice or for the CRUD portion of a vehicle-related feature.
---

# Feature slice

## Establish the contract

Turn the description into a short field and operation list. Record required and optional values, JSON types, constraints, uniqueness scope, and relationships. Keep explicit API names and operations. For an unspecified standalone entity, use the vehicle CRUD surface. For a child entity, keep the requested nesting and scope each lookup to its parent. State necessary assumptions and proceed.

Read the corresponding implementation and tests in `backend/src/{main,test}/java/com/example/dealership/vehicle/` and `frontend/src/app/vehicles/`. Read `app.routes.ts` and `app.html` before adding navigation. The vehicle module is the reference for structure, error handling, and UI behavior. The entity description is the reference for business rules.

## Implement the backend

- Add a package with the entity, repository, request and response records, mapper, transactional service and controller. Preserve the separation between HTTP validation, service rules, and persistence. Keep the existing response conventions, including 201 with Location for creation, 204 for deletion, and the shared ProblemDetail handlers.
- Add the next Flyway migration. Hibernate validates the schema on startup, so align nullability, names, lengths, enum storage, precision, and foreign keys with the entity.
- Normalize input before validation when the description ignores surrounding spaces or treats blank optional values as absent. Do it in the request record's compact constructor, so that the length limits, the stored value, and the uniqueness check all see the same value. Leave values that the description does not mention unchanged.
- For uniqueness, apply the same comparison rule on create and update. Exclude the current record on update. Back the rule with a database constraint that uses the same comparison: when the rule ignores case or formatting, store the normalized key in its own column, set it in the entity whenever the source value changes, and put the unique constraint on that column. A plain unique constraint on the raw column does not protect a case-insensitive rule.
- Use `saveAndFlush` for writes that a constraint can reject, so a concurrent duplicate fails inside the service call and the shared handler returns 409 instead of an error at commit.
- For a child entity, use a foreign key to the parent, load the child by both its ID and the parent ID (404 for a child of another parent), and make the parent delete behavior explicit in the migration (for example `ON DELETE CASCADE`). Test it against the database. Preserve the current delete behavior unless the contract changes it.
- When a rule allows only one record per scope in some states (for example one active record per type), back it with a lock or a constraint, not only a service check. Lock the parent row (`PESSIMISTIC_WRITE`) before the check, or use a constraint the database supports.
- A migration must also work on a database with existing rows. Add a new `NOT NULL` column with a `DEFAULT`, do not depend on seed rows, and do not add seed data unless the request asks for it. Keep earlier migrations unchanged.
- Follow the vehicle tests for one complete round trip and each important failure. Use a repository test for constraints or mappings that controller mocks cannot prove.

## Implement the frontend

- Follow the vehicle list, detail and shared create/edit form with standalone components, `httpResource` reads, and signal forms. Use the shared route parameter and ProblemDetail helpers. Register a lazy route and navigation for a standalone slice, or wire the UI into its parent for a child slice.
- Match request and response types to the API. Map the form deliberately in both directions. Preserve zero and false. Convert empty optional inputs to the API representation. Give enum values usable labels and use controls suited to each field.
- Use the vehicle screens for loading, empty, and error states. Keep field errors at their inputs and general errors visible. After a write, show the saved values or refresh the list. Cover the form's create and edit payloads, optional values, and server errors with frontend tests.

## Verify

Follow every operation from the route and user action to the database and back. Compare the field list with the migration, DTOs, client types, form, and tests. Run `./test.sh` as documented in the repository and run `npm run build` in `frontend/` to compile all routes and templates. Start the backend and call each new route with curl: one success, one 400 with field errors, one 404, and each 409 rule, including the normalized variant (for example a different letter case) and parallel duplicate requests, which must return 409 and never 500. Fix any failures. Report the assumptions and the checks that ran.
