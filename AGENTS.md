# AGENTS.md

## Prerequisites
- Java 21
- Docker (for local PostgreSQL)
- Maven wrapper included (`./mvnw`)

## Setup & Run

1. Copy `.env.example` to `.env` and fill in DB credentials (and R2 values if needed)
2. Start PostgreSQL: `docker compose up -d`
3. Run the app: `./mvnw spring-boot:run`

The app listens on `${APP_PORT}` (default 8080). On first boot, `DataSeeder` inserts 6 default departments into a fresh database.

## Build & Test

```bash
./mvnw compile          # compile only
./mvnw test             # run tests
./mvnw package -DskipTests  # build jar without tests
```

The only existing test is `OpdqueueApplicationTests.contextLoads()` -- a `@SpringBootTest` that requires a running PostgreSQL. If no DB is available, the test will fail.

## Architecture

- **Package root:** `lk.opdqueue`
- **Framework:** Spring Boot 4.0.6, Java 21, PostgreSQL 16, STOMP/WebSocket, Thymeleaf
- **DB:** PostgreSQL via JPA/Hibernate with `ddl-auto=update` (schema auto-managed)
- **No Spring Security** -- all endpoints are open

### Key package layout

| Package | Role |
|---------|------|
| `config/` | `WebSocketConfig`, `R2Config` (conditional on `app.r2.enabled=true`), `DataSeeder` |
| `entity/` | JPA entities: `Patient`, `QueueTicket`, `Department`, `Doctor`, `Staff`, `Appointment` |
| `repository/` | Spring Data JPA repositories |
| `service/` | `QueueService` (core business logic), `PatientService`, `AppointmentService`, `SlipGeneratorService`, `R2StorageService` |
| `controller/` | REST controllers + MVC controllers for Thymeleaf templates |
| `strategy/` | `QueueStrategy` interface, `PriorityQueueStrategy` (active), `FifoQueueStrategy` |
| `observer/` | `DisplayBoardNotifier`, `PatientNotifier` -- push updates via `SimpMessagingTemplate` |
| `dto/` | Request/response DTOs with Jakarta Validation annotations |
| `util/` | `QRCodeGenerator` (ZXing), `TicketNumberGenerator` |
| `exception/` | Custom exceptions + `GlobalExceptionHandler` (`@RestControllerAdvice`) |

### WebSocket

- STOMP endpoint: `/ws` (with SockJS fallback)
- In-memory broker at `/topic`
- Application destination prefix: `/app`
- CORS: all origins allowed
- Topic `/topic/queue/{deptId}` receives full `DisplayBoardResponse` (current ticket, next tickets, total waiting)
- Topic `/topic/ticket/{ticketNumber}` receives full `QueueStatusResponse` (status, position, peopleAhead, wait time)
- `QueueEventListener` has 3 hooks: `onTicketCalled`, `onTicketIssued` (new), `onQueueUpdated`
- `issueTicket`, `callNext`, `complete`, `markNoShow`, and `resetDepartmentQueue` all broadcast WebSocket updates

### PDF & R2 Storage

- `app.r2.enabled=false` by default. When `false`, PDF slips are generated locally to `generated/` dir.
- Set `app.r2.enabled=true` and provide R2 credentials in `.env` to upload slips to Cloudflare R2 instead.

## Conventions

- Entity IDs use `Long` (auto-increment) for `Department`, `Doctor`, `Staff`; `UUID` for `Patient`, `QueueTicket`, `Appointment`
- `TicketStatus` enum controls the queue state machine: `REGISTERED -> WAITING -> CALLED -> IN_PROGRESS -> COMPLETED | NO_SHOW | CANCELLED`
- Slip PDF generation uses iText7; QR codes use ZXing
- HTML templates in `src/main/resources/templates/` use Thymeleaf
- The `websocket/` package directory exists but is empty -- all WebSocket messaging is driven via observers, not custom handlers

## Notes

- Hibernate `ddl-auto=update` means entities drive the schema; never drop tables manually without understanding this
- `QueueTicketRepository` has custom `@Query` methods -- check those before adding complex queries
- `DataSeeder` is idempotent (checks `count > 0`), but it runs on every startup
