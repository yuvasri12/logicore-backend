# LogiCore

LogiCore is a logistics operations app. A shipment is registered, received and shelved in the warehouse, loaded, dispatched and delivered. Every step is role-controlled and audited. Customers can track their shipment without logging in.

This repository is the **backend** (Spring Boot REST API). The Angular frontend is in a separate repository.

## Features

- Login with JWT and four roles: Admin, Ops Manager, Warehouse, Dispatcher
- Role-based access: each role sees and can call only what it needs
- Shipments with a strict workflow: NEW, RECEIVED, IN_WAREHOUSE, LOADED, IN_TRANSIT, DELIVERED, CANCELLED
- Warehouse: receive freight, assign storage location (zone, rack, bin)
- Dispatch: assign vehicle and destination
- Customers: create, edit, activate, deactivate
- User management for Admin: create, edit role, enable, disable
- Append-only audit log of important actions (Admin only)
- Public tracking by tracking number, with a timeline and rate limiting
- All user-facing messages in messages.properties; one global exception handler; SLF4J logging with request ids

## Tech stack

- Java, Spring Boot 2.7.18, Spring Security (stateless JWT), Spring Data JPA
- PostgreSQL 16 (H2 in tests)
- JUnit 5, MockMvc

## Run locally

1. Create a PostgreSQL database named `logicore`.
2. Start the app from IntelliJ (main class `LogiCoreOmsApplication`) or run `mvn spring-boot:run`.
3. The API runs on http://localhost:8080.

Local defaults (laptop only, never use in production):

- Database user `postgres`, password `admin`
- First Admin: `yuvasris531@gmail.com` / `ChangeMe@123`
- Demo users (same password): ops.manager@logicore.local, warehouse@logicore.local, dispatcher@logicore.local

Override with environment variables: LOGICORE_DB_URL, LOGICORE_DB_USER, LOGICORE_DB_PASSWORD, LOGICORE_JWT_SECRET, LOGICORE_ADMIN_EMAIL, LOGICORE_ADMIN_NAME, LOGICORE_ADMIN_PASSWORD, LOGICORE_DEMO_USERS.

Production: run with `--spring.profiles.active=prod`. That profile has no defaults, so the app refuses to start unless every value above is set, and demo users are off.

## Main APIs

| Area | Endpoints |
| --- | --- |
| Auth | POST /api/auth/login, GET /api/auth/me |
| Shipments | GET and POST /api/shipments, GET, PUT, DELETE /api/shipments/{id}, PATCH /api/shipments/{id}/status, POST /api/shipments/{id}/load, POST /api/shipments/{id}/deliver |
| Warehouse | POST /api/warehouse/receive, POST /api/warehouse/assign-location |
| Dispatch | POST /api/dispatch |
| Customers | GET and POST /api/customers, PUT /api/customers/{id}, POST /api/customers/{id}/activate, POST /api/customers/{id}/deactivate |
| Users (Admin) | GET and POST /api/users, PATCH /api/users/{id} |
| Audit (Admin) | GET /api/audit |
| Public tracking | GET /api/tracking/{trackingNumber} (no login) |

## Demo flow

1. Log in as Admin and create a shipment for a customer.
2. Warehouse: receive the freight, then assign zone, rack and bin.
3. Dispatch the shipment with a vehicle and destination.
4. Mark it delivered.
5. Open the public tracking page with the tracking number and see the timeline.
6. Admin: open the Audit Log and see who did each step.

## Tests

Run all tests from IntelliJ (right-click `src/test` and choose Run Tests). They use an in-memory H2 database and cover the workflow, role access, tracking, customers, users and audit.

## Roadmap

Disabled-user token revocation, shipment pagination, optimistic locking, password change and reset, Flyway migrations, hosted live demo.
