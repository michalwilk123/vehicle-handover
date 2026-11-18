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
- For uniqueness, apply the same comparison rule on create and update. Exclude the current record on update. Back the rule with a database constraint that also covers concurrent writes. For relationships, check the parent and keep ID lookups within its scope. Preserve the current delete behavior unless the contract changes it.
- Follow the vehicle tests for one complete round trip and each important failure. Use a repository test for constraints or mappings that controller mocks cannot prove.

## Implement the frontend

- Follow the vehicle list, detail and shared create/edit form with standalone components, `httpResource` reads, and signal forms. Use the shared route parameter and ProblemDetail helpers. Register a lazy route and navigation for a standalone slice, or wire the UI into its parent for a child slice.
- Match request and response types to the API. Map the form deliberately in both directions. Preserve zero and false. Convert empty optional inputs to the API representation. Give enum values usable labels and use controls suited to each field.
- Use the vehicle screens for loading, empty, and error states. Keep field errors at their inputs and general errors visible. After a write, show the saved values or refresh the list. Cover the form's create and edit payloads, optional values, and server errors with frontend tests.

## Verify

Follow every operation from the route and user action to the database and back. Compare the field list with the migration, DTOs, client types, form, and tests. Run `./test.sh` as documented in the repository and run `npm run build` in `frontend/` to compile all routes and templates. Fix any failures. Report the assumptions and the checks that ran.
