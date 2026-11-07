# Barangay Jibolo Child Immunization Scheduling and Monitoring System
Java 17 + Spring Boot 3 + Thymeleaf + MySQL + Bootstrap 5.

## Run
1. Start MySQL. Edit `src/main/resources/application.properties` (username/password). The database `jibolo_immunization` and all tables are created automatically.
2. `mvn spring-boot:run`  ->  open http://localhost:8080
3. Default staff accounts (change the passwords after first login): `bhw` / `bhw12345`, `midwife` / `midwife123`.
   Staff may also sign up themselves using the staff code `app.staff-code` (default `JIBOLO2026`). Parents sign up freely.

## Roles
- Parent: register child (with consent), auto schedule, request reschedule, notifications, printable card.
- Midwife: today's list, vaccinate (temp/weight/height/remarks), mark absent, schedule visits, approve/decline requests, search children.
- BHW: barangay monitoring and reports (CSV/print), emergency mass reschedule with auto notification, children, accounts.
Schedule follows the EPI card: BCG & HepB at birth; Penta/OPV/PCV 6-10-14 weeks; IPV 14 weeks & 9 months; MR/MMR 9 & 12 months.

## Upgrading from the earlier version
The `child` table is now `child_records` and the DHW role is now BHW. Run `DROP DATABASE jibolo_immunization;` in phpMyAdmin once, then start the app so everything is recreated.
Default accounts: `bhw` / `bhw12345`, `midwife` / `midwife123`.

## Production upgrade (v3)
Local run is unchanged (`mvn spring-boot:run`, dev profile, XAMPP). For publishing read **DEPLOY.md**.
Because of the new record-version columns, if you see a database error on first start, run `DROP DATABASE jibolo_immunization;` once and start again.
