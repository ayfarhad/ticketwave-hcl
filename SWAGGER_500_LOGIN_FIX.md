# 500 Internal Server Error - Login Troubleshooting

**Last Updated**: 2026-03-03  
**Issue**: Getting 500 Internal Server Error when logging in  
**Error Type**: `POST /api/auth/login`

---

## Problem Summary

```
Error: 500 Internal Server Error
Message: Internal server error or database connection failed
```

This occurs when:
- ❌ User doesn't exist in database
- ❌ Database connection failed
- ❌ Password hashing error
- ❌ JWT secret not configured
- ❌ Application not fully initialized

---

## Quick Fix (Step-by-Step)

### Step 1: Check If Application is Running Properly

**Open Terminal and verify**:
```powershell
# Check if application is still running
# Look for this message in terminal:
# "Started TicketWaveApplication in X.XXX seconds"
```

**If application crashed**, restart it:
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
mvn spring-boot:run
```

**Wait for this message**:
```
Started TicketWaveApplication in 12.035 seconds (process running for 15.123)
```

✅ **Don't proceed until you see this message**

---

### Step 2: Check Database Initialization

**The database needs initial users to be created**. The app uses H2 (in-memory database) which is fresh every time it starts.

**Check in application logs for**:
```
Executing: create table users
Executing: create table routes
Executing: create table seats
...
Flyway migration complete
```

**If you see migration errors**, the database didn't initialize. Check the terminal output.

---

### Step 3: Create a User First (Register)

Before logging in, you need to create a user account.

**Endpoint**: `POST /api/auth/register`

**Go to Swagger**: `http://localhost:8080/swagger-ui.html`

**Find**: **POST /api/auth/register**

**Click "Try it out"**

**Enter Request Body**:
```json
{
  "username": "testuser",
  "email": "testuser@example.com",
  "password": "TestPass@123",
  "role": "PASSENGER"
}
```

**Click "Execute"**

**Expected Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "testuser@example.com",
    "role": "PASSENGER",
    "enabled": true
  },
  "message": "User registered successfully"
}
```

✅ **If you got 201, user is created**

❌ **If you got 500 on register too**, skip to Debugging section below

---

### Step 4: Now Try Login with Registered User

**Endpoint**: `POST /api/auth/login`

**Enter Request Body** (use same username/password from registration):
```json
{
  "username": "testuser",
  "password": "TestPass@123"
}
```

**Click "Execute"**

**Expected Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlciI6InRlc3R1c2VyIiwicm9sZSI6IlBBU1NFTkdFUiIsImlhdCI6MTY3ODAxMTQ0NSwiZXhwIjoxNjc4MDk3ODQ1fQ.abcdefghijklmnop",
    "expiresIn": 86400,
    "tokenType": "Bearer"
  },
  "message": "Login successful"
}
```

✅ **Success! You're logged in**

---

## Common Causes & Detailed Fixes

### Cause 1: User "passenger1" Doesn't Exist (Most Common)

**Error**:
```json
{
  "success": false,
  "message": "Internal server error",
  "timestamp": "2026-03-03T10:30:00.123Z"
}
```

**Reason**: The application starts with a clean H2 database. Pre-configured users like "passenger1" don't exist unless seeded.

**Fix**:
1. **Option A**: Register a new user first (see Step 3 above)
2. **Option B**: Seed pre-configured users on startup (see "Seed Data" section below)

---

### Cause 2: Database Connection Failed

**Error in Terminal**:
```
org.h2.jdbc.JdbcSQLException: Database is read-only
```

**Fix**:
1. Close application: `Ctrl+C` in terminal
2. Delete H2 database file (if using file-based):
   ```
   rm -Force ~/.h2.server.properties
   rm -Force ~/ticketwave.mv.db
   ```
3. Restart application:
   ```powershell
   mvn spring-boot:run
   ```

---

### Cause 3: JWT Secret Not Configured

**Error in Terminal**:
```
JWT_SECRET environment variable not set
```

**Fix**:
Set JWT secret before running:
```powershell
$env:JWT_SECRET='your-secret-key-min-32-chars-length-required'
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.10'
mvn spring-boot:run
```

**Or in `application.yml`** (already configured):
```yaml
jwt:
  secret: "your-super-secret-jwt-key-that-is-at-least-32-characters-long-for-security"
  expiration: 86400
```

---

### Cause 4: Password Hashing Error

**Error in Terminal**:
```
BCryptPasswordEncoder initialization failed
```

**Cause**: Tomcat fork issue with annotation processors

**Fix**: Already fixed in `pom.xml` with:
```xml
<configuration>
  <fork>true</fork>
  <release>21</release>
</configuration>
```

If still failing:
```powershell
mvn clean compile -DskipTests
mvn spring-boot:run
```

---

### Cause 5: Application Didn't Initialize Properly

**Error**: API returns 500 on all endpoints

**Fix**:

1. **Check if application fully started**:
   ```
   Look for: "Started TicketWaveApplication in X.XXX seconds"
   ```

2. **If not started, check for errors** in terminal output

3. **Common startup errors**:
   - **Port already in use**: 
     ```powershell
     netstat -ano | findstr :8080
     # Kill process using port 8080
     taskkill /PID <PID> /F
     ```
   
   - **Database locked**:
     ```powershell
     mvn clean
     mvn spring-boot:run
     ```
   
   - **Compilation errors**:
     ```powershell
     mvn clean compile -DskipTests
     ```

---

## Detailed Debugging Steps

### Step 1: Enable Debug Logging

**Create/Edit `application.yml`** and add:
```yaml
logging:
  level:
    com.ticketwave: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

**Restart application**:
```powershell
mvn spring-boot:run
```

**Now check terminal for detailed error messages when login fails**

---

### Step 2: Check Terminal Output for Exact Error

**When calling login endpoint, look for error in terminal**:

**Example 1**: User not found
```
[DEBUG] User 'passenger1' not found in database
```

**Fix**: Register user first (Step 3 above)

---

**Example 2**: Password mismatch
```
[DEBUG] Password validation failed for user 'passenger1'
```

**Fix**: Use correct password

---

**Example 3**: JWT generation error
```
[ERROR] Failed to generate JWT token: JWT_SECRET not configured
```

**Fix**: Set JWT_SECRET environment variable

---

**Example 4**: Database error
```
[ERROR] java.sql.SQLException: Database 'default' not found
```

**Fix**: Database didn't initialize, restart app

---

### Step 3: Test Database Connection

**Open H2 Console** (if enabled):
```
http://localhost:8080/h2-console
```

**Login with**:
```
Driver: org.h2.Driver
URL: jdbc:h2:mem:testdb
User: sa
Password: (leave blank)
```

**Run query to check if users table exists**:
```sql
SELECT * FROM users;
```

**Expected Output**:
```
ID | USERNAME | EMAIL | PASSWORD_HASH | ROLE | ENABLED | CREATED_AT | UPDATED_AT
(empty or with registered users)
```

**If "users" table doesn't exist**, database migration failed.

---

## Seeding Test Data (Recommended)

To avoid having to register users manually, seed them on startup.

### Option 1: SQL Insert via H2 Console

**URL**: `http://localhost:8080/h2-console`

**Insert test users**:
```sql
-- Password: "Pass@123" hashed with BCrypt
-- Hashed value: $2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6

INSERT INTO users (id, username, email, password_hash, role, enabled, created_at, updated_at)
VALUES 
  (1, 'passenger1', 'passenger1@example.com', '$2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6', 'PASSENGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'passenger2', 'passenger2@example.com', '$2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6', 'PASSENGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'admin', 'admin@example.com', '$2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

✅ Now login will work with:
```json
{
  "username": "passenger1",
  "password": "Pass@123"
}
```

---

### Option 2: Create Data.sql File

**Create file**: `src/main/resources/data.sql`

```sql
-- Insert test users
INSERT INTO users (id, username, email, password_hash, role, enabled, created_at, updated_at) VALUES
(1, 'passenger1', 'passenger1@example.com', '$2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6', 'PASSENGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'passenger2', 'passenger2@example.com', '$2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6', 'PASSENGER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'admin', 'admin@example.com', '$2a$10$slYQmyNdGzin7olVN3p36OPST9/PgBkqquzi.Ee403IUgO19PK.P6', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test routes
INSERT INTO routes (id, origin, destination, transport_type, created_at, updated_at) VALUES
(1, 'New York', 'Los Angeles', 'FLIGHT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Boston', 'Miami', 'FLIGHT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'San Francisco', 'Seattle', 'BUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test schedules
INSERT INTO schedules (id, route_id, departure_time, arrival_time, base_price, created_at, updated_at) VALUES
(1, 1, '2026-03-15 08:00:00', '2026-03-15 14:00:00', 299.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, '2026-03-20 10:00:00', '2026-03-20 16:00:00', 199.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 3, '2026-03-18 12:00:00', '2026-03-18 18:00:00', 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test seats
INSERT INTO seats (id, schedule_id, seat_number, class, status, created_at, updated_at) VALUES
(1, 1, '1A', 'BUSINESS', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, '1B', 'BUSINESS', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, '2A', 'ECONOMY', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, '2B', 'ECONOMY', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, '1A', 'BUSINESS', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 3, '1A', 'STANDARD', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

**Restart application**:
```powershell
mvn spring-boot:run
```

✅ Now all test data will be automatically seeded

---

## Complete Troubleshooting Checklist

- [ ] **Step 1**: Application is running (see "Started TicketWaveApplication...")
- [ ] **Step 2**: Wait 10 seconds after startup for database to initialize
- [ ] **Step 3**: Check application logs for errors (in terminal)
- [ ] **Step 4**: Register a new user via `POST /api/auth/register`
- [ ] **Step 5**: Login with registered credentials
- [ ] **Step 6**: If still 500, check H2 console for database state
- [ ] **Step 7**: Check JWT_SECRET is configured in `application.yml`
- [ ] **Step 8**: Clear browser cache (`Ctrl+Shift+Delete`)
- [ ] **Step 9**: Refresh Swagger UI (`F5`)
- [ ] **Step 10**: Try login again

---

## Quick Reference: Test Accounts (After Seeding)

| Username | Password | Role | Status |
|----------|----------|------|--------|
| passenger1 | Pass@123 | PASSENGER | ✅ Active |
| passenger2 | Pass@123 | PASSENGER | ✅ Active |
| admin | Pass@123 | ADMIN | ✅ Active |

---

## Browser DevTools Debugging (F12)

1. **Open DevTools**: Press `F12`
2. **Go to Network tab**
3. **Click on failed login request**
4. **Go to Response tab** - Check exact error message
5. **Go to Console tab** - Look for JavaScript errors

**Look for response like**:
```json
{
  "success": false,
  "message": "User 'passenger1' not found",
  "timestamp": "2026-03-03T10:30:00.123Z"
}
```

---

## If Still Getting 500 Error

**Last Resort Troubleshooting**:

```powershell
# 1. Stop application
# Ctrl+C in terminal

# 2. Clean build
mvn clean

# 3. Compile only
mvn compile -DskipTests

# 4. Delete H2 database
Remove-Item -Path "$env:USERPROFILE\.h2.server.properties" -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:USERPROFILE\ticketwave*" -Force -ErrorAction SilentlyContinue

# 5. Start fresh
mvn spring-boot:run

# 6. Wait for database to initialize (30 seconds)
# Look for "Started TicketWaveApplication..."

# 7. Register a user first
# POST /api/auth/register with new credentials

# 8. Then login
# POST /api/auth/login with those same credentials
```

---

## Expected Terminal Output When Starting Successfully

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/

2026-03-03 10:30:45.123  INFO 12345 --- [main] c.t.TicketWaveApplication : Starting TicketWaveApplication v1.0.0
2026-03-03 10:30:45.456  INFO 12345 --- [main] c.t.TicketWaveApplication : No active profile set, falling back to 1 default profile: "default"
2026-03-03 10:30:47.789  INFO 12345 --- [main] o.f.c.internal.database.base.Database : Found existing schema version: 1
2026-03-03 10:30:48.123  INFO 12345 --- [main] o.h.jpa.internal.util.LogHelper : HHH000204: Processing PersistenceUnitInfo [name: default]
2026-03-03 10:30:49.456  INFO 12345 --- [main] o.s.s.web.DefaultSecurityFilterChain : Will secure any request with [...15 security filters...]
2026-03-03 10:30:50.789  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port(s): 8080 (http)
2026-03-03 10:30:51.123  INFO 12345 --- [main] c.t.TicketWaveApplication : Started TicketWaveApplication in 6.123 seconds (JVM running for 7.456)

✅ Application is ready to accept requests!
```

---

**Status**: ✅ Follow these steps to resolve 500 error on login!

If still not working, check the **SWAGGER_403_FIX.md** for additional authorization troubleshooting.
