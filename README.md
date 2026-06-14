## OPD Queue

A real-time outpatient department (OPD) queue management system for hospitals. Patients register, get queue tickets, and track their position live. Staff manage the queue from an admin panel while a public display board shows the current serving number.

## Features

- **Patient registration** - walk-in and pre-registered patients
- **Queue ticket issuance** - with department selection, emergency priority, and print slip generation
- **Live display board** - shows "Now Serving", up-next list, and waiting count with real-time updates
- **Patient status page** - patients scan a QR code to see their position, people ahead, and estimated wait time
- **Admin panel** - manage departments, doctors, staff; call next, complete, and mark no-show
- **WebSocket push** - instant updates across display board and patient pages with no refresh

## How to Run

### 1. Prerequisites

- **Java 21**
- **Docker** (for PostgreSQL)

### 2. Setup

```bash
# Clone and enter the project
cd opdqueue

# Copy and fill environment variables
cp .env.example .env

# Start PostgreSQL
docker compose up -d

# Run the application
./mvnw spring-boot:run
```

### 3. Open in browser

| Page          | URL                             |
| ------------- | ------------------------------- |
| Reception     | http://localhost:8080           |
| Display Board | http://localhost:8080/display/1 |
| Admin Panel   | http://localhost:8080/admin     |

## PDF Slips — Local vs Cloud (R2)

When a ticket is issued, a PDF slip is generated. By default, slips are saved **locally** to the `generated/` directory.

To store slips in the cloud (so they survive server restarts and are accessible from any machine), enable Cloudflare R2:

```bash
# In your .env file:
R2_ENDPOINT=https://<account>.r2.cloudflarestorage.com
R2_ACCESS_KEY=your-access-key
R2_SECRET_KEY=your-secret-key
R2_BUCKET_NAME=opdqueue-slips
R2_PUBLIC_URL=https://pub-xxx.r2.dev
```

Then set `app.r2.enabled=true` in `application.properties`.

## Tech Stack

Java 21 · Spring Boot 4.0 · PostgreSQL 16 · Thymeleaf · STOMP WebSocket · iText7 PDF · ZXing QR · Cloudflare R2

## TODO

- [ ] **Authentication** — add login for staff and admin (likely Asgardeo IDP or a similar OIDC provider)
- [ ] Role-based access control (receptionist vs doctor vs admin)
- [ ] Doctor consultation view — see called patients and mark as in-progress / complete
- [ ] SMS or email notifications when a patient's turn is near
- [ ] Multi-language support (Sinhala / Tamil / English)
