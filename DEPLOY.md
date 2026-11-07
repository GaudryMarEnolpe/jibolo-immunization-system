# Publishing the Jibolo Immunization System

## 1. How the settings are organised
| File | Used when | Purpose |
|---|---|---|
| `application.properties` | always | shared, safe settings (port, session, headers) |
| `application-dev.properties` | default, on your laptop | XAMPP database, sample passwords, detailed errors |
| `application-prod.properties` | `SPRING_PROFILES_ACTIVE=prod` | live server: **all secrets come from environment variables** |

## 2. Environment variables on the live server
| Variable | Example | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | switches to the live settings |
| `DB_URL` | `jdbc:mysql://HOST:3306/jibolo_immunization?useSSL=true&serverTimezone=Asia/Manila` | from your cloud MySQL provider |
| `DB_USER` / `DB_PASSWORD` | | create a dedicated DB user, not root |
| `STAFF_CODE` | a long random phrase | required for midwife/BHW self sign-up |
| `BOOTSTRAP_BHW_PASSWORD` | 10+ characters | **first start only**: creates the `bhw` account, then delete this variable |
| `PORT` | set by most hosts | defaults to 8081 |

Never commit passwords to GitHub. Keep them only in the host's "Environment" / "Variables" page.

## 3. Build and run
- Jar: `mvn -DskipTests package` then `java -jar target/immunization-1.0.0.jar`
- Docker: `docker build -t jibolo .` (the included `Dockerfile` is ready to use on Render, Railway, Fly.io, Azure, AWS, or any VPS).
- Health check URL for the host: `/actuator/health`

## 4. Moving the data away from XAMPP
1. phpMyAdmin -> `jibolo_immunization` -> **Export** -> SQL.
2. Create an empty MySQL 8 database at your host and **Import** that file (or start empty and let the app create the tables).
3. Treat the dump as confidential; it contains children's records.

## 5. Go-live checklist
- [ ] HTTPS enabled by the host (the prod profile already forces HTTPS and secure cookies)
- [ ] Managed database with **automatic daily backups** (test a restore once)
- [ ] Changed the first BHW password, then removed `BOOTSTRAP_BHW_PASSWORD`
- [ ] Dedicated DB user with a strong password; database not open to the whole internet
- [ ] A new `STAFF_CODE`, shared only with real staff
- [ ] Tested every role: a parent must not open `/midwife/...` or `/bhw/...` (they get "Access not allowed")
- [ ] Tested on a phone
- [ ] Agreed with the Municipal Health Office who may see the data. Child health records are sensitive personal information under the Philippine Data Privacy Act (RA 10173); ask your LGU's Data Protection Officer about consent, retention and access rules before using real data.

## 6. Later improvements
- Replace `ddl-auto=update` with **Flyway** migrations and `ddl-auto=validate`, so database changes are versioned and reviewed.
- Add an audit log (who changed which record) and email/SMS-free reminders if needed.
- Run the app on 2 instances behind the host's load balancer only after moving the login-lockout counter to the database or Redis (it is in memory now).
