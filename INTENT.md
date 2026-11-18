# Vehicle handover and customers

## Purpose

Give dealership staff a vehicle document checklist in the application. Show whether the vehicle is ready for handover.

## Customers

| ID | Requirement | Source |
| --- | --- | --- |
| C-01 | Show a customer list. Show the first name, last name, email address, and telephone number for each customer. Show the Polish tax identification number (NIP) if it has a value. | [Meeting](BRIEF.md#L109) |
| C-02 | Let staff add, see, change, and delete customer records. Keep the first name, last name, email address, telephone number, and NIP. | [Meeting and API](BRIEF.md#L109), [API](BRIEF.md#L34) |
| C-03 | Do not let two customer records have the same email address. Apply this rule when staff add or change a customer. | [Meeting](BRIEF.md#L109) |
| C-04 | Use the vehicle API conventions for the customer API. Sort the list by ID. Return HTTP 409 if the email address belongs to a different customer. | [API](BRIEF.md#L20), [Meeting](BRIEF.md#L109) |

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/customers` | 200, customer list |
| GET | `/api/customers/{id}` | 200, one customer |
| POST | `/api/customers` | 201, new customer and a `Location` header |
| PUT | `/api/customers/{id}` | 200, changed customer |
| DELETE | `/api/customers/{id}` | 204 |

## Vehicle documents

| ID | Requirement | Source |
| --- | --- | --- |
| V-01 | Let staff record the registration choice in the vehicle form. The choice tells the application if the dealership will register the vehicle. Use customer registration as the default for new and existing vehicles. | [Meeting](BRIEF.md#L81) |
| D-01 | Show the documents for one vehicle on its detail page. Include rejected documents in the list. | [API](BRIEF.md#L30), [Meeting](BRIEF.md#L99) |
| D-02 | Let staff add a document from the vehicle detail page. The request must include the type, number, and issue date. The expiry date is optional. Set the initial status to `PENDING`. | [API](BRIEF.md#L31), [Meeting](BRIEF.md#L85) |
| D-03 | Return HTTP 400 for an issue date after the current date. | [Meeting](BRIEF.md#L95) |
| D-04 | A document is active when its status is `PENDING` or `VERIFIED`. Prevent more than one active document of each type for the same vehicle. Return HTTP 409 if staff try to add a second active document of the same type. | [API](BRIEF.md#L26), [Meeting](BRIEF.md#L99) |
| D-05 | Let staff change the status of a `PENDING` document to `VERIFIED` or `REJECTED`. Keep a rejected document in the document history. Keep the status of a rejected document at `REJECTED`. See the assumptions for changes from `VERIFIED`. | [Meeting](BRIEF.md#L85), [Final decision](BRIEF.md#L111-L115) |
| D-06 | Let staff add a new document after staff reject the active document of that type. Start the new document in `PENDING`. | [Final decision](BRIEF.md#L111-L115), [API](BRIEF.md#L31) |
| D-07 | Return HTTP 409 for a status change that does not agree with the status rules. | [API](BRIEF.md#L26) |
| D-08 | Let staff change a document status on the vehicle detail page. Show the new status. Update the readiness result after staff add a document or change its status. | [Meeting](BRIEF.md#L85), [UI scope](BRIEF.md#L15) |

Use the table to select the required document types. [Meeting](BRIEF.md#L71-L81)

| Document type | API value | Required for handover |
| --- | --- | --- |
| Sales agreement | `SALES_AGREEMENT` | Yes |
| Invoice | `INVOICE` | Yes |
| Certificate of conformity (COC) | `COC` | Yes |
| Motor liability insurance policy | `OC_POLICY` | Yes |
| Power of attorney for vehicle registration | `REGISTRATION_POA` | Only if the dealership will register the vehicle |

The API accepts the document types in this table and the statuses `PENDING`, `VERIFIED`, and `REJECTED`. [API](BRIEF.md#L22-L24)

| Method | Path | Request | Result |
| --- | --- | --- | --- |
| GET | `/api/vehicles/{vehicleId}/documents` | — | 200, document list |
| POST | `/api/vehicles/{vehicleId}/documents` | `type`, `number`, `issueDate`, optional `expiryDate` | 201, new document and a `Location` header |
| PUT | `/api/vehicles/{vehicleId}/documents/{documentId}/status` | JSON object with `status` | 200, document with its new status |

Include `id`, `type`, `number`, `issueDate`, `expiryDate`, and `status` in each document response.

Use the vehicle API error format for all new routes. Return HTTP 400 with field errors for invalid data. An unknown document type or status value is invalid data. Return HTTP 404 if a record does not exist on the requested path. This includes a document ID from a different vehicle. [API conventions](BRIEF.md#L20-L34)

## Handover readiness

| ID | Requirement | Source |
| --- | --- | --- |
| R-01 | Use the planned handover date of the vehicle. Set `ready` to false if the date has no value. If the vehicle has no planned handover date, add a problem with reason `NO_HANDOVER_DATE` and a null `documentType`. | [Meeting](BRIEF.md#L103), [API](BRIEF.md#L43-L49) |
| R-02 | For each required type, find its active document. If there is no active document, add the type to `missingDocumentTypes`. | [Meeting](BRIEF.md#L71-L81), [Meeting](BRIEF.md#L99) |
| R-03 | If an active document of a required type has status `PENDING`, add a problem with reason `NOT_VERIFIED`. | [Meeting](BRIEF.md#L85), [API](BRIEF.md#L49) |
| R-04 | Check expiry for active documents of required types. Use the planned handover date as the reference date. If the planned date has no value, use the current date. Add an `EXPIRED` problem if the expiry date is before the reference date. An expiry date equal to the reference date passes the check. | [Meeting](BRIEF.md#L91), [API](BRIEF.md#L49) |
| R-05 | Set `ready` to true if both arrays are empty. Set `ready` to false if either array contains an item. | [Meeting](BRIEF.md#L71), [Meeting](BRIEF.md#L85-L103) |
| R-06 | On the vehicle detail page, show if the vehicle is ready for handover. Show the missing document types and the problems if the vehicle is not ready. | [Meeting](BRIEF.md#L101-L103) |

Use `GET /api/vehicles/{vehicleId}/handover-readiness`. Return HTTP 200 with a Boolean `ready`. Return `missingDocumentTypes` and `problems` as arrays. For each problem, give its `documentType` and `reason`. [API](BRIEF.md#L33-L49)

For this version, use the order in the document type table for both readiness arrays. For one type, put `NOT_VERIFIED` before `EXPIRED`. Put `NO_HANDOVER_DATE` last. Sort the document list by ID.

## Questions and assumptions

| Question for the customer | Assumption for this version |
| --- | --- |
| Can staff change a verified document to rejected? Can staff send the same status again? | Accept a change from `PENDING` to `VERIFIED` or `REJECTED`. Accept a change from `VERIFIED` to `REJECTED`. This lets staff replace a verified document if it expires or has an error. Return HTTP 409 for all other requests with one of the three status values. |
| Which types need an expiry date? Which types can have one? | The API makes the field optional for all types. Accept an OC policy without an expiry date. A document without an expiry date passes the expiry check. |
| Which time zone defines the current date? Must the issue date be on or before the expiry date and the planned handover date? | Use the dealership date in `Europe/Warsaw`. Compare the issue date only with the current date. |
| Which date does the application use to check expiry if the planned handover date has no value? Can one type have two problems? | Report missing types and status problems without a handover date. Check expiry against the planned handover date. If the planned handover date has no value, use the current date in `Europe/Warsaw`. Report both problems if both checks fail for one active document. |
| Which customer fields and formats does the dealership need? How do staff identify a company? | Use one customer record format. Apply the customer field rules below. Use NIP for a company. |
| What format does the document number use? | Ignore spaces at the start and end. The number must have 1 to 100 characters after this step. Use no type-specific format. |
| Does the director approve the final rule for rejected documents? Do staff need a rejection reason or a separate audit log? | Apply Anna's final rule for this version. Keep the rejected record in the document history. |
| Do different letter cases or spaces make two email addresses different? | Ignore spaces at the start and end of the email address. Keep the letter case in the saved address. Compare email addresses without regard to letter case. A customer record can keep its email address during an update. |
| What happens to the documents when staff delete a vehicle? | Delete the documents with the vehicle. Keep the result of the existing vehicle DELETE operation. |
| How do existing vehicle API clients send the registration choice? | Add `dealershipRegisters` to vehicle requests and responses. If POST omits the field or sends null, use false. If PUT omits the field or sends null, keep the saved value. |
| Is it necessary for staff to keep document files in this version? Is a separate action necessary to mark a vehicle as handed over? | Keep the document data only. Show the readiness result. |

Anna gives the rule for this version at the end of the meeting. To replace a rejected document, staff add a new record. [Final decision](BRIEF.md#L111-L115)

### Customer field rules for this version

| Field | Rule |
| --- | --- |
| First name and last name | Both fields must have a value. Each field must have 1 to 50 characters. |
| Email address | The address must pass Jakarta Bean Validation `@Email`. Accept a maximum of 254 characters. |
| Telephone number | The number must have nine digits. A space or hyphen can separate two digits. Enter the number without a country code. |
| NIP | Accept an empty value. If the field has a value, the value must have ten characters. Each character must be a digit. Do not do a checksum check. |

Ignore spaces at the start and end of each customer field. Apply the length limits after this step.

### Expiry dates and the API example

The meeting gives these facts about expiry dates. [Meeting](BRIEF.md#L91)

- A sales agreement or invoice has no expiry date.
- An OC policy has an expiry date.
- A registration power of attorney usually has an expiry date.

The API example shows `EXPIRED` and `NO_HANDOVER_DATE` together. The meeting gives no other date for the expiry check. This version uses the current date in that case. This lets the response include both problems. [API example](BRIEF.md#L36-L49), [Meeting](BRIEF.md#L91)

## Out of scope

- SMS messages and an SMS gateway.
- Links between customers and vehicles.
- Upload and storage of document scans.
- A separate action to complete handover.
- Rejection reasons and a separate audit log.
- New user accounts and access rules.

[Meeting](BRIEF.md#L85), [Meeting](BRIEF.md#L101-L109), [Document API](BRIEF.md#L30-L33)

## Code checks

The checks refer to starter commit `f92a26c`. **Present** means the starter has the listed behavior. **New** means the starter has no code for that behavior.

| IDs | Starter evidence | Acceptance check |
| --- | --- | --- |
| C-01–C-04 | **New.** [VehicleController](backend/src/main/java/com/example/dealership/vehicle/VehicleController.java) has the CRUD routes and status codes. [VehicleService](backend/src/main/java/com/example/dealership/vehicle/VehicleService.java) checks for duplicate VINs. [V1 schema](backend/src/main/resources/db/migration/V1__vehicle.sql) has a unique VIN constraint. [Angular routes](frontend/src/app/app.routes.ts) load only vehicles. | Run the five customer operations. Make sure that the customer list shows its fields. Try to add a duplicate email address. Try to change a second customer to that address. Make sure that both responses have HTTP status 409. Change a customer without changing its email address. Make sure that the response has HTTP status 200. Send invalid field values. Make sure that the response has HTTP status 400 and field errors. |
| V-01, R-01 | **Present.** [VehicleRequest](backend/src/main/java/com/example/dealership/vehicle/VehicleRequest.java), [VehicleMapper](backend/src/main/java/com/example/dealership/vehicle/VehicleMapper.java), and the [V1 schema](backend/src/main/resources/db/migration/V1__vehicle.sql) support a nullable planned handover date. [Vehicle form tests](frontend/src/app/vehicles/vehicle-form.spec.ts) compare the complete request body. Include the new registration field in those expectations. **New.** Registration choice and the readiness check. | Create a vehicle without a registration choice. Make sure that the customer registers it. Change the choice. Reload the vehicle. Make sure that the dealership registers it. Send a vehicle update without the registration field. Make sure that the choice stays the same. Set the planned handover date to null. Make sure that the API returns `NO_HANDOVER_DATE` and `ready: false`. |
| D-01–D-08 | **New.** [VehicleController](backend/src/main/java/com/example/dealership/vehicle/VehicleController.java) has no document routes. [GlobalExceptionHandler](backend/src/main/java/com/example/dealership/error/GlobalExceptionHandler.java) defines the existing 400, 404, and 409 response format. [Vehicle detail](frontend/src/app/vehicles/vehicle-detail.html) has an empty Documents section. | Add a document with today's issue date. Make sure that the new document has status `PENDING`. Try to add a second document of the same type. Make sure that the response has HTTP status 409. Change the first document's status to `VERIFIED`. Try to add a second document of the same type. Make sure that the response has HTTP status 409. Send a future issue date. Make sure that the response has HTTP status 400. Reject a different pending document. Request a status change to `VERIFIED`. Make sure that the response has HTTP status 409. Add a replacement. Make sure that the list contains both records. Reject a verified document. Make sure that staff can add its replacement. Use a different vehicle ID for the status request. Make sure that the response has HTTP status 404. Delete a vehicle with documents. Make sure that the response has HTTP status 204. Make sure that no document records remain for the vehicle. |
| R-02–R-06 | **New.** [Vehicle API](frontend/src/app/vehicles/vehicle-api.ts) and the [vehicle detail page](frontend/src/app/vehicles/vehicle-detail.html) have no readiness code. | Use the cases below. Make sure that the vehicle detail page shows the API result. |

Start each case with four verified documents, one for each type that is always required. The customer registers the vehicle. Set a planned handover date. Use dates that pass all checks. Apply only the change in the row.

| Change | Expected result |
| --- | --- |
| The customer registers the vehicle. | `ready: true`. Both arrays are empty. |
| The dealership registers the vehicle. | `ready: false`. `missingDocumentTypes` contains one item, `REGISTRATION_POA`. |
| The dealership registers the vehicle. A `REGISTRATION_POA` document has status `VERIFIED` and no expiry date. | `ready: true`. Both arrays are empty. |
| The customer registers the vehicle. An optional `REGISTRATION_POA` has status `PENDING`. | `ready: true`. Both arrays are empty. |
| All `COC` records have status `REJECTED`. | `ready: false`. `missingDocumentTypes` contains one item, `COC`. `problems` is empty. |
| The `COC` has status `PENDING`. | `ready: false`. `missingDocumentTypes` is empty. `problems` includes `NOT_VERIFIED` for `COC`. |
| The OC policy expires before the planned handover date. | `ready: false`. `missingDocumentTypes` is empty. `problems` includes `EXPIRED` for `OC_POLICY`. |
| The OC policy expires on or after the planned handover date. | `ready: true`. Both arrays are empty. |
| The OC policy has no expiry date. | `ready: true`. Both arrays are empty. |
| The OC policy has status `PENDING` and expires before the planned handover date. | `ready: false`. `problems` includes `NOT_VERIFIED` and `EXPIRED` for `OC_POLICY`. |
| The vehicle has no planned handover date. The OC policy expires today or later. | `ready: false`. `missingDocumentTypes` is empty. The only problem is `NO_HANDOVER_DATE`, with a null document type. |
| The vehicle has no planned handover date. The OC policy expired before today. No `COC` exists. | `ready: false`. `missingDocumentTypes` contains `COC`. Report `EXPIRED` for `OC_POLICY`. Report `NO_HANDOVER_DATE` with a null document type. |

The starter tests passed. The backend passed 29 tests. The frontend passed 9 tests. The acceptance checks above need the new feature code.
