# 🛵 Courier Shifts – Booking System

A small full-stack app that lets a delivery company manage **courier shifts**: create couriers, publish shifts per delivery zone, and let couriers book, reschedule or cancel those shifts.

It was built as the **system under test for a Senior Quality Engineer technical exercise**. The app has no automated tests on purpose. Your job is to write them.

---

## Tech stack

| Layer    | Technology                                                    |
|----------|---------------------------------------------------------------|
| Backend  | Java 21, Spring Boot 3.5 (Web, Data JPA, Validation), Flyway  |
| Database | PostgreSQL 16                                                 |
| Frontend | Plain HTML + CSS + vanilla JavaScript served by Nginx         |
| Runtime  | Docker Compose                                                |

```
.
├── backend/            # Spring Boot REST API
│   └── src/main/resources/db/migration   # Schema + seed data (Flyway)
├── frontend/           # Static SPA + nginx config (proxies /api → backend)
└── docker-compose.yml
```

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine + Compose v2)
- Free local ports: **3000** (UI), **8080** (API), **5432** (Postgres)

You don't need Java, Maven or Node on your machine. Everything is built inside Docker.

---

## Running the project

```bash
# from the repository root
docker compose up --build
```

The first build downloads dependencies and takes a few minutes. Once all three containers are healthy:

| What              | URL                                         |
|-------------------|---------------------------------------------|
| Web UI            | http://localhost:3000                       |
| REST API          | http://localhost:8080/api                   |
| Swagger UI        | http://localhost:8080/swagger-ui.html       |
| OpenAPI spec      | http://localhost:8080/v3/api-docs           |
| Health check      | http://localhost:8080/actuator/health       |
| PostgreSQL        | `localhost:5432` · db `courier_shifts` · user `courier` · password `courier` |

The UI also works through the proxy at `http://localhost:3000/api/...`.

### Useful commands

```bash
docker compose up -d --build      # start in background
docker compose logs -f backend    # follow backend logs
docker compose down               # stop (data is kept)
docker compose down -v            # stop AND wipe the database (re-seeds on next start)
```

> **Resetting data:** `docker compose down -v && docker compose up -d` gives you a fresh database with the seed data again.

### Port conflicts

If a port is already taken, change the **left** side of the mapping in `docker-compose.yml` (e.g. `"5433:5432"`).

---

## Seed data

On first start the DB is populated with:

| Courier        | Email               | Vehicle | Active |
|----------------|---------------------|---------|--------|
| Alice Johnson  | alice@example.com   | BIKE    | ✅     |
| Bob Smith      | bob@example.com     | SCOOTER | ✅     |
| Carla Gomez    | carla@example.com   | CAR     | ✅     |
| David Lee      | david@example.com   | BIKE    | ❌     |

Five shifts are created relative to the moment the DB was initialised: upcoming shifts in DOWNTOWN, NORTH, SOUTH and EAST (the EAST one is already **full**), plus one **past** shift in WEST. Some bookings already exist.

---

## Domain & business rules

### Couriers
- `name` required, 2–100 chars
- `email` required, valid format, **unique** (case-insensitive, stored lowercase)
- `phone` required, 7–20 digits; may start with `+` and contain spaces or dashes
- `vehicleType` one of `BIKE`, `SCOOTER`, `CAR`
- `active` defaults to `true`
- **Deactivating** a courier cancels their confirmed bookings on shifts that **have not started yet**, freeing those slots. Bookings on started or past shifts are kept
- A courier **with any booking history cannot be deleted**. Deactivate it instead.

### Shifts
- `zone` one of `DOWNTOWN`, `NORTH`, `SOUTH`, `EAST`, `WEST`
- `startTime` must be **in the future**
- `endTime` must be after `startTime`; duration must be **between 1 and 12 hours**
- `capacity` between **1 and 50**
- Derived fields: `bookedCount`, `availableSlots`, `status` (`OPEN`, `FULL`, `IN_PROGRESS`, `COMPLETED`)
- A shift that has **already started cannot be edited**
- While a shift has confirmed bookings, its **times and zone cannot change**, and **capacity cannot drop below** the number of confirmed bookings
- A shift **with any bookings cannot be deleted**

### Bookings
A courier can book a shift only if **all** of these hold:
1. The courier exists and is **active**
2. The shift exists and **has not started**
3. The courier doesn't already have a confirmed booking on **this shift**
4. The courier has no confirmed booking that **overlaps in time** with this shift
5. The shift is **not full**

Capacity and the overlap rule are enforced with DB row locks (on the shift and on the courier), so they hold under
concurrent requests, for booking, rescheduling and cancelling alike.

Cancelling:
- Only `CONFIRMED` bookings can be cancelled, and only **before the shift starts**
- A cancelled booking frees up the slot, and the courier can book the same shift again

Rescheduling:
- Only `CONFIRMED` bookings can be rescheduled
- The booking and the target shift must exist
- The booking's current shift must **not have started** (as for cancelling)
- The target shift must be **different from the current shift**
- The courier must be **active**
- The target shift must **not have started**
- The courier must have no **other** confirmed booking that overlaps the target shift. The booking being moved is **excluded** from the overlap check
- The target shift must **not be full**
- The booking keeps its identity: the same booking is moved to the new shift, the old slot is freed and the new slot is taken. No new booking row is created and `createdAt` is unchanged

---

## REST API

Base URL: `http://localhost:8080/api`. All bodies are JSON and all timestamps are **ISO-8601 UTC** (e.g. `2030-01-15T09:00:00Z`).

### Endpoints

| Method | Path                          | Description                                   |
|--------|-------------------------------|-----------------------------------------------|
| GET    | `/meta`                       | Allowed zones and vehicle types               |
| GET    | `/couriers?active={bool}`     | List couriers (optional filter)               |
| GET    | `/couriers/{id}`              | Get courier                                   |
| POST   | `/couriers`                   | Create courier → `201`                        |
| PUT    | `/couriers/{id}`              | Update courier                                |
| DELETE | `/couriers/{id}`              | Delete courier → `204`                        |
| GET    | `/couriers/{id}/bookings`     | Bookings of a courier                         |
| GET    | `/shifts?zone=&from=&to=`     | List shifts (filter by zone / start time range) |
| GET    | `/shifts/{id}`                | Get shift                                     |
| POST   | `/shifts`                     | Create shift → `201`                          |
| PUT    | `/shifts/{id}`                | Update shift                                  |
| DELETE | `/shifts/{id}`                | Delete shift → `204`                          |
| GET    | `/shifts/{id}/bookings`       | Bookings of a shift                           |
| GET    | `/bookings?courierId=&shiftId=&status=` | List bookings                       |
| GET    | `/bookings/{id}`              | Get booking                                   |
| POST   | `/bookings`                   | Book a shift → `201`                          |
| POST   | `/bookings/{id}/cancel`       | Cancel a booking                              |
| POST   | `/bookings/{id}/reschedule`   | Move a confirmed booking to another shift    |

Full schemas are in **Swagger UI**.

### Examples

```bash
# Create a courier
curl -s -X POST http://localhost:8080/api/couriers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Eve Adams","email":"eve@example.com","phone":"+1-555-0199","vehicleType":"SCOOTER"}'

# Create a shift
curl -s -X POST http://localhost:8080/api/shifts \
  -H 'Content-Type: application/json' \
  -d '{"zone":"DOWNTOWN","startTime":"2030-01-15T09:00:00Z","endTime":"2030-01-15T13:00:00Z","capacity":2}'

# Book it
curl -s -X POST http://localhost:8080/api/bookings \
  -H 'Content-Type: application/json' \
  -d '{"courierId":1,"shiftId":6}'

# Cancel booking 1
curl -s -X POST http://localhost:8080/api/bookings/1/cancel

# Reschedule booking 1 to shift 7
curl -s -X POST http://localhost:8080/api/bookings/1/reschedule \
  -H 'Content-Type: application/json' \
  -d '{"shiftId":7}'
```

### Error format

Every error uses the same body:

```json
{
  "timestamp": "2030-01-01T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Shift is full",
  "path": "/api/bookings",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```
`fieldErrors` is present only for bean-validation failures.

| Status | When                                                                 |
|--------|----------------------------------------------------------------------|
| 400    | Validation failure, malformed JSON, invalid enum or param, bad time range |
| 404    | Resource not found                                                   |
| 405    | HTTP method not supported                                            |
| 409    | Business-rule violation (duplicate email, full shift, overlap, …)    |
| 500    | Unexpected error                                                     |

---

## Web UI

Open http://localhost:3000. Three tabs:

- **Shifts**: list shifts (filter by zone and upcoming only), create, edit, delete, and **Book** a shift for an active courier
- **Couriers**: list, create, edit (including activating or deactivating), delete, and jump to a courier's bookings
- **Bookings**: list bookings (filter by courier and status), reschedule confirmed future bookings, and cancel them

The reschedule dialog uses a searchable shift picker (a custom ARIA combobox, not a native `<select>`).
It loads upcoming shifts asynchronously, excludes the booking's current shift, shows full shifts as
disabled, filters by shift id or zone, and supports keyboard navigation.

Success and error messages from the API appear as a notification in the top-right corner. Times are shown and entered in the **browser's local timezone** and sent to the API as UTC.

---

## Running the backend without Docker (optional)

Requires JDK 21+ and Maven 3.9+, plus a Postgres instance (you can run only the DB with `docker compose up -d db`):

```bash
cd backend
mvn spring-boot:run
```

Connection settings can be overridden with `DB_URL`, `DB_USER` and `DB_PASSWORD`.

---

## 🧪 The exercise (for candidates)

Use this application to build an **automated test suite**. Choose any language, framework and tooling you like. We're interested in:

1. **API tests** for the REST endpoints and business rules above
2. **UI / end-to-end tests** for the main user flows in the web app
3. How you **structure** the project, **manage test data** and keep tests **independent and repeatable**
4. How someone else would **run** your suite (a README, and Docker or CI configuration are a plus)
5. Any **bugs or inconsistencies** you find. Write them up as you would in a real bug report.

Put your tests in a new top-level folder (e.g. `/tests`) or in a separate repository.
