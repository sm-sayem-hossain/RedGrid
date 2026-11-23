# RedGrid — Real-Time Blood Donor Network & Management System

Pure Java (Swing + Core Java) desktop application. No external frameworks or databases —
all persistence via Java Object Serialization into local `.dat` files.

## Run

```bash
./run.sh          # or manually:
javac -d out $(find src -name '*.java')
java -cp out redgrid.Main
```

Requires JDK 17+.

## First login

The admin account is seeded automatically on first run:

| ID     | Password |
|--------|----------|
| A-0001 | `admin`  |

Register Donor / Recipient accounts from the login window.

## Architecture

```
redgrid/
  model/        User (abstract) ← Donor / Recipient / Admin,
                BloodGroup (ABO/Rh compatibility), BloodRequest, DonationRecord,
                EligibilityValidator (90-day rule)
  service/      Store (registry + auth + save), MatchingEngine,
                AlertDispatcher (daemon thread → donor pop-ups)
  persistence/  FilePersistenceManager (users.dat / requests.dat / records.dat)
  ui/           Theme (deck tokens: paper #F4EFEB, surface, ink, accent #E2574C),
                LoginFrame, RecipientConsole, DonorHub, AdminConsole
```

## Demo flow (two instances, side by side)

1. Instance 1 → register **Donor** (district Dhaka, group O-), log in.
2. Instance 2 → register **Recipient** (district Dhaka, group A+), log in.
3. Recipient: broadcast an urgent request — compatible, eligible donors are matched
   (ABO/Rh + district + 90-day cooldown) and alerted.
4. Donor window: emergency alert pop-up → **Accept**. The donation is logged, the
   donor enters the 90-day rest period, and the request auto-fulfils when enough bags are covered.
5. Admin console: audit users, monitor requests, view the donation log.

Data lives in `./data/*.dat` (created at runtime).
