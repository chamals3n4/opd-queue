# OPD Queue — Try-Out Tutorial

This guide walks you through a full end-to-end test of the application: adding staff, doctors, registering 20 patients, issuing queue tickets, and watching the live display board update in real time.

---

## 1. Start the application

```bash
docker compose up -d      # start PostgreSQL
./mvnw spring-boot:run    # start the app
```

Wait until you see `Started OpdqueueApplication` in the terminal, then open:

| URL | What it is |
|-----|------------|
| `http://localhost:8080/login` | Login page |
| `http://localhost:8080` | Reception desk |
| `http://localhost:8080/admin` | Admin panel |
| `http://localhost:8080/display/1` | Live display board (OPD) |
| `http://localhost:8080/status/{ticketNumber}` | Patient self-service status |

---

## 2. Log in

Go to `http://localhost:8080/login`.

**Default admin credentials (seeded on first boot):**

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |

You will land on the Reception page.

---

## 3. Open the display board (second tab)

Before doing anything else, open a second browser tab and go to:

```
http://localhost:8080/display/1
```

Keep this tab visible alongside the admin/reception tab. Every action you take will update it live via WebSocket — you will see the ticket number, up-next list, and waiting count change in real time.

---

## 4. Add doctors (Admin → Doctors)

Go to `http://localhost:8080/admin` → click **Doctors** in the left sidebar → click **+ Add Doctor**.

Add each of these one at a time:

| # | Full Name | Specialization | Room |
|---|-----------|---------------|------|
| 1 | Dr. Kamal Perera | General Physician | Room 01 |
| 2 | Dr. Nisha Fernando | Internal Medicine | Room 02 |
| 3 | Dr. Roshan Silva | Cardiology | Room 03 |
| 4 | Dr. Amara Jayasinghe | Paediatrics | Room 04 |
| 5 | Dr. Dilani Wickramasinghe | Gynaecology | Room 05 |

---

## 5. Add staff accounts (Admin → Staff)

Go to **Staff** in the sidebar → click **+ Add Staff**.

This creates login accounts for the OPD team. Add all four roles so you can test each one:

| Full Name | Role | Username | Password |
|-----------|------|----------|----------|
| Saman Kumara | RECEPTIONIST | `saman` | `pass123` |
| Nimali Ranasinghe | NURSE | `nimali` | `pass123` |
| Dr. Roshan Silva | DOCTOR | `roshan` | `pass123` |
| Priya Mendis | ADMIN | `priya` | `pass123` |

> **Role behaviour:** All authenticated users currently reach the same pages — roles are stored for future access control. You can sign out (top-right) and sign back in as any of these accounts to verify they can log in.

---

## 6. Register 20 patients

Patients can be registered in two places:

- **Admin panel** → Patients tab → **+ Register Patient** (back-office registration)
- **Reception page** (`http://localhost:8080`) → **Register New Patient** form at the top (front-desk walk-in)

Use a mix of both to see the difference. The patient type field controls whether they appear as Walk-in or Pre-registered on the ticket slip.

### Register via Admin panel (Pre-registered patients — 10)

Go to **Admin → Patients → + Register Patient** and add these:

| NIC | Full Name | DOB | Gender | Contact | Type |
|-----|-----------|-----|--------|---------|------|
| 198805102345 | Kamal Bandara | 1988-05-10 | Male | 0771234567 | Pre-registered |
| 199203207890 | Nisha Perera | 1992-03-20 | Female | 0712345678 | Pre-registered |
| 197611154321 | Roshan De Silva | 1976-11-15 | Male | 0761234567 | Pre-registered |
| 200108256789 | Amara Jayawardena | 2001-08-25 | Female | 0751234567 | Pre-registered |
| 198407039876 | Thilak Fernando | 1984-07-03 | Male | 0781234567 | Pre-registered |
| 199512180123 | Chamari Weerasinghe | 1995-12-18 | Female | 0701234567 | Pre-registered |
| 197203224567 | Sunil Rathnayake | 1972-03-22 | Male | 0771234568 | Pre-registered |
| 200305149012 | Dilini Herath | 2003-05-14 | Female | 0712345679 | Pre-registered |
| 196809071234 | Anura Dissanayake | 1968-09-07 | Male | 0761234568 | Pre-registered |
| 199801285678 | Sachini Madushani | 1998-01-28 | Female | 0751234568 | Pre-registered |

### Register via Reception page (Walk-in patients — 10)

Go to `http://localhost:8080`. Use the **Register New Patient** section:

| NIC | Full Name | DOB | Gender | Contact | Type |
|-----|-----------|-----|--------|---------|------|
| 200512103456 | Kasun Rajapaksha | 2005-12-10 | Male | 0781234568 | Walk-in |
| 199706227891 | Malini Gunasekara | 1997-06-22 | Female | 0701234568 | Walk-in |
| 198103186543 | Pradeep Liyanage | 1981-03-18 | Male | 0771234569 | Walk-in |
| 200209144321 | Hiruni Senanayake | 2002-09-14 | Female | 0712345680 | Walk-in |
| 197504251098 | Gamini Amarasinghe | 1975-04-25 | Male | 0761234569 | Walk-in |
| 199309308765 | Sanduni Kumarasinghe | 1993-09-30 | Female | 0751234569 | Walk-in |
| 196612112345 | Piyal Seneviratne | 1966-12-11 | Male | 0781234569 | Walk-in |
| 200107056789 | Thilini Pathirana | 2001-07-05 | Female | 0701234569 | Walk-in |
| 197801299876 | Nuwan Wijesinghe | 1978-01-29 | Male | 0771234570 | Walk-in |
| 199415173214 | Chamali Peiris | 1994-04-17 | Female | 0712345681 | Walk-in |

---

## 7. Issue queue tickets at Reception

This is the core daily flow. Go to `http://localhost:8080`.

In the **Issue Ticket** section:
1. Type the patient's NIC in the NIC field and press Tab or click away — the patient name auto-fills.
2. The Department is already set to OPD.
3. Leave **Emergency** unchecked for normal patients.
4. Click **Issue Ticket**.

A ticket slip opens in a new tab (or prints). The display board in your second tab updates instantly.

**Issue tickets for these patients in order (watch the display board queue grow):**

1. Kamal Bandara — `198805102345`
2. Nisha Perera — `199203207890`
3. Kasun Rajapaksha — `200512103456`
4. Roshan De Silva — `197611154321`
5. Malini Gunasekara — `199706227891`
6. Amara Jayawardena — `200108256789`
7. Pradeep Liyanage — `198103186543`
8. Thilak Fernando — `198407039876`

### Try an emergency ticket

Issue a ticket for **Gamini Amarasinghe** (`197504251098`) and tick the **Emergency** checkbox. His ticket will jump to position 1 in the queue, ahead of everyone already waiting. Watch the display board reorder immediately.

---

## 8. Work the queue (Admin → Queue Control)

Go to `http://localhost:8080/admin` → **Queue Control**.

### Call the next patient

Click the **Call Next Patient** card on the right. The "Now Serving" panel on the left updates to the called ticket, and the display board shows it as the current number.

### Complete or mark no-show

In the **Waiting Queue** table below, you will see action buttons on the currently called ticket:
- **Complete** — marks the consultation done, decrements the waiting count.
- **No-show** — marks the patient as absent, removes them from the queue.

Work through several patients: call → complete → call → complete. Watch the display board waiting count drop each time.

### Reset the queue

If you want to start fresh, click **Reset Queue** (top-right of Queue Control). A confirmation dialog will appear. Confirming cancels all remaining waiting tickets and zeros the count.

---

## 9. Track your status as a patient

When a ticket is issued, the slip contains a QR code and a URL in the format:

```
http://localhost:8080/status/{ticketNumber}
```

Open that URL in a third browser tab (or scan the QR on the printed slip). It shows:
- Current ticket status (Waiting / Called / Completed)
- Queue position
- Number of people ahead
- Estimated wait time

This page updates automatically via WebSocket — when the staff calls the patient or marks them complete, the status page refreshes without reloading.

---

## 10. Live display board

The display board at `http://localhost:8080/display/1` is designed to be shown on a large screen in the waiting area. It shows:

- **Now Serving** — the ticket number currently being seen
- **Up Next** — the next 5 tickets in priority order (emergency first, then by position)
- **Waiting count** — total patients still in queue
- **Live clock** — HH:MM:SS, updates every second

No login is required for the display board — it is intentionally public so a wall-mounted screen can show it without credentials.

---

## 11. Try different user accounts

Sign out from the top-right and sign in as the accounts you created in Step 5:

| Username | What to test |
|----------|-------------|
| `saman` (Receptionist) | Issue tickets at Reception, register walk-in patients |
| `nimali` (Nurse) | Call next patient, mark complete/no-show in Queue Control |
| `roshan` (Doctor) | View the queue, see who is next |
| `priya` (Admin) | Full access — manage doctors, staff, reset queue |

All roles currently reach the same pages. The role field is stored in the database and available for future permission layering.

---

## Quick reference

| Action | Where |
|--------|-------|
| Register patient | Reception `/` or Admin → Patients |
| Issue ticket | Reception `/` — Issue Ticket section |
| Call next / complete / no-show | Admin → Queue Control |
| Add doctors | Admin → Doctors |
| Add / edit staff accounts | Admin → Staff |
| Reset entire queue | Admin → Queue Control → Reset Queue |
| Live display board | `/display/1` (no login needed) |
| Patient status page | `/status/{ticketNumber}` (no login needed) |
| Print ticket slip | Opens automatically after issuing, or `/api/tickets/{ticketNumber}/slip` |
