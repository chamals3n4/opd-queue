# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start PostgreSQL (required before running)
docker compose up -d

# Run the application (dev mode with hot-reload)
./mvnw spring-boot:run

# Build
./mvnw compile
./mvnw package -DskipTests

# Run tests (requires a running PostgreSQL)
./mvnw test
```

The only test is `OpdqueueApplicationTests.contextLoads()` — a `@SpringBootTest` that requires a live database. Tests will fail without one.

## Architecture

Spring Boot 4.0.6 / Java 21 / PostgreSQL 16 / Thymeleaf / STOMP WebSocket. No Spring Security — all endpoints are open.

Schema is managed by Hibernate `ddl-auto=update` (entities drive the schema). On first boot, `DataSeeder` inserts 6 default departments (idempotent — checks `count > 0` before inserting, but runs every startup).

### Core queue flow

`QueueService` is the central service. The ticket lifecycle is a state machine:
```
REGISTERED → WAITING → CALLED → IN_PROGRESS → COMPLETED
                                             → NO_SHOW
                      → NO_SHOW
             → CANCELLED
```

When any state change happens, `QueueService` calls all `QueueEventListener` implementations:
- `DisplayBoardNotifier` — pushes `DisplayBoardResponse` to `/topic/queue/{deptId}`
- `PatientNotifier` — pushes `QueueStatusResponse` to `/topic/ticket/{ticketNumber}`

Emergency tickets sort before normal tickets. The active ordering strategy is `PriorityQueueStrategy` (emergency first, then by `queuePosition`). `FifoQueueStrategy` exists but is not wired up.

### WebSocket

- STOMP endpoint: `/ws` (SockJS fallback enabled)
- In-memory broker on `/topic`, application prefix `/app`
- All updates are pushed from the observer layer after service calls — there are no custom `@MessageMapping` handlers

### PDF slips & R2

`SlipGeneratorService` generates a PDF slip (iText7) with a QR code (ZXing) when a ticket is issued. By default (`app.r2.enabled=false`) slips are written to `generated/` locally. Set `app.r2.enabled=true` and fill R2 env vars to upload to Cloudflare R2 instead. `R2Config` is conditional on that property.

### Entity ID conventions

- `Long` (auto-increment): `Department`, `Doctor`, `Staff`
- `UUID`: `Patient`, `QueueTicket`, `Appointment`

### Key non-obvious details

- `QueueTicketRepository` has custom `@Query` methods — check there before writing new queries.
- `QueueService.issueTicket` saves the ticket twice: once to get the ID, then again after setting the slip URL.
- The `websocket/` package directory exists in the source tree but is empty — do not add handlers there without understanding the observer pattern already in use.
- `app.base-url` is used to build the QR code URL embedded in the PDF slip (points to the patient status page).

## Pages

| URL | Purpose |
|-----|---------|
| `http://localhost:8080` | Reception (issue tickets, register patients) |
| `http://localhost:8080/display/{deptId}` | Live display board |
| `http://localhost:8080/admin` | Admin panel (manage departments, doctors, staff, queue) |
| `http://localhost:8080/status/{ticketNumber}` | Patient self-service status page (linked from QR code) |
