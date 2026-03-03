# Functional Test Cases Documentation - TicketWave Backend

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Test Framework**: JUnit 5, Mockito, Spring Boot Test  
**Coverage Target**: >80% code coverage

---

## Table of Contents

1. [Test Case Structure](#1-test-case-structure)
2. [Test Environment Setup](#2-test-environment-setup)
3. [Authentication & Authorization Test Cases](#3-authentication--authorization-test-cases)
4. [Search & Route Discovery Test Cases](#4-search--route-discovery-test-cases)
5. [Inventory Management Test Cases](#5-inventory-management-test-cases)
6. [Booking Management Test Cases](#6-booking-management-test-cases)
7. [Payment Processing Test Cases](#7-payment-processing-test-cases)
8. [Refunds Management Test Cases](#8-refunds-management-test-cases)
9. [Admin & User Management Test Cases](#9-admin--user-management-test-cases)
10. [Common Features Test Cases](#10-common-features-test-cases)
11. [Integration Test Cases](#11-integration-test-cases)
12. [Performance & Load Test Cases](#12-performance--load-test-cases)
13. [Security Test Cases](#13-security-test-cases)

---

## 1. Test Case Structure

### 1.1 Standard Test Case Format

Each test case follows this template:

```
Test Case ID: [TC-MODULE-XXX]
Title: [Concise description]
Module: [Auth/Search/Inventory/Booking/Payment/Admin/Common]
Type: [Positive/Negative/Edge Case]
Priority: [P0-Critical / P1-High / P2-Medium / P3-Low]

Preconditions:
- List of prerequisites

Test Steps:
1. Step 1
2. Step 2
...

Expected Result:
- HTTP Status: [code]
- Response Body: [JSON structure or key assertions]
- Side Effects: [Database changes, cache updates, etc.]

Test Data:
- Key data inputs required

Notes:
- Any additional context or dependencies
```

### 1.2 Test Type Definitions

| Type | Description |
|------|-------------|
| **Positive** | Happy path; valid inputs, expected success |
| **Negative** | Invalid inputs, business rule violations, error handling |
| **Edge Case** | Boundary conditions, rare scenarios, stress limits |
| **Integration** | Multi-module interaction, end-to-end workflows |

### 1.3 Priority Levels

| Priority | Impact | Execution |
|----------|--------|-----------|
| **P0** | Critical; system cannot function | Must pass before release |
| **P1** | High; core functionality broken | Must pass in QA |
| **P2** | Medium; feature degraded | Should pass before release |
| **P3** | Low; nice-to-have, workaround exists | Can defer to next sprint |

---

## 2. Test Environment Setup

### 2.1 Test Configuration

**File**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  h2:
    console:
      enabled: true
  flyway:
    enabled: true
```

### 2.2 Test Data Fixtures

**Location**: `src/test/java/com/ticketwave/TestDataFactory.java`

Provides factory methods for consistent test data:
- `createUser()` - Returns User entity
- `createRoute()` - Returns Route entity
- `createSchedule()` - Returns Schedule entity
- `createSeat()` - Returns Seat entity
- `createBooking()` - Returns Booking entity
- `createPayment()` - Returns Payment entity

### 2.3 Test Database Initialization

- **Scope**: Each test class uses `@DataJpaTest` or `@SpringBootTest`
- **Isolation**: Tests run in transactions; rollback after each test
- **Seed Data**: Optional `@Sql` annotations for pre-populated data

### 2.4 Mock & Spy Configuration

**Technology**: Mockito with MockitoAnnotations

- `@Mock`: External dependencies (email, payment gateway)
- `@InjectMocks`: Service under test
- `@Spy`: When partial mocking needed (rare)

---

## 3. Authentication & Authorization Test Cases

### 3.1 User Registration Tests

#### TC-AUTH-001: Successful User Registration
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Application running
- No user with email "test@example.com" exists

**Test Steps**:
1. POST `/api/auth/register` with payload:
   ```json
   {
     "username": "testuser",
     "email": "test@example.com",
     "password": "SecurePass123"
   }
   ```

**Expected Result**:
- HTTP Status: 201 Created
- Response: `{ success: true, data: { userId, username, email, role: "PASSENGER" } }`
- Database: User entity created with bcrypt-hashed password
- JWT Token: Returned in response or Authorization header

**Test Data**: Valid email, 8+ character password, unique username

---

#### TC-AUTH-002: Registration with Duplicate Email
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- User with email "existing@example.com" already exists

**Test Steps**:
1. POST `/api/auth/register` with email "existing@example.com"

**Expected Result**:
- HTTP Status: 409 Conflict
- Response: `{ success: false, message: "Email already registered" }`
- Database: No new user created

---

#### TC-AUTH-003: Registration with Invalid Email Format
- **Type**: Negative
- **Priority**: P2

**Preconditions**:
- None

**Test Steps**:
1. POST `/api/auth/register` with email "invalidemail"

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, errors: { email: "Invalid email format" } }`

---

#### TC-AUTH-004: Registration with Short Password
- **Type**: Negative
- **Priority**: P2

**Preconditions**:
- Minimum password length: 8 characters

**Test Steps**:
1. POST `/api/auth/register` with password "Short1"

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response includes password validation error

---

#### TC-AUTH-005: Registration with Missing Required Field
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. POST `/api/auth/register` without "username" field

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, errors: { username: "Username is required" } }`

---

### 3.2 User Login Tests

#### TC-AUTH-006: Successful Login
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- User "testuser" exists with password "SecurePass123"

**Test Steps**:
1. POST `/api/auth/login` with payload:
   ```json
   {
     "username": "testuser",
     "password": "SecurePass123"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response includes JWT token with claims: `sub`, `username`, `role`, `exp`
- Token valid for 24 hours

**Test Data**: Matching credentials

---

#### TC-AUTH-007: Login with Incorrect Password
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- User "testuser" exists

**Test Steps**:
1. POST `/api/auth/login` with username "testuser" and password "WrongPass"

**Expected Result**:
- HTTP Status: 401 Unauthorized
- Response: `{ success: false, message: "Invalid credentials" }`
- No token issued

---

#### TC-AUTH-008: Login with Non-Existent User
- **Type**: Negative
- **Priority**: P1

**Test Steps**:
1. POST `/api/auth/login` with username "nonexistent"

**Expected Result**:
- HTTP Status: 401 Unauthorized
- Response: `{ success: false, message: "Invalid credentials" }`

---

#### TC-AUTH-009: Login with Disabled User
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- User exists with `enabled = false`

**Test Steps**:
1. POST `/api/auth/login` with disabled user credentials

**Expected Result**:
- HTTP Status: 403 Forbidden
- Response: `{ success: false, message: "User account disabled" }`

---

### 3.3 JWT Validation Tests

#### TC-AUTH-010: Access Protected Endpoint with Valid JWT
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Valid JWT token obtained from login

**Test Steps**:
1. GET `/api/booking/list` with header: `Authorization: Bearer <JWT_TOKEN>`

**Expected Result**:
- HTTP Status: 200 OK
- Request processed with user context available

---

#### TC-AUTH-011: Access Protected Endpoint without JWT
- **Type**: Negative
- **Priority**: P0

**Test Steps**:
1. GET `/api/booking/list` without Authorization header

**Expected Result**:
- HTTP Status: 401 Unauthorized
- Response: `{ success: false, message: "Missing or invalid token" }`

---

#### TC-AUTH-012: Access Protected Endpoint with Expired JWT
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- JWT token expired (created 25+ hours ago)

**Test Steps**:
1. GET `/api/booking/list` with header: `Authorization: Bearer <EXPIRED_JWT>`

**Expected Result**:
- HTTP Status: 401 Unauthorized
- Response: `{ success: false, message: "Token expired" }`

---

#### TC-AUTH-013: Access Protected Endpoint with Invalid JWT Signature
- **Type**: Negative
- **Priority**: P1

**Test Steps**:
1. GET `/api/booking/list` with malformed JWT (tampered signature)

**Expected Result**:
- HTTP Status: 401 Unauthorized
- Response: `{ success: false, message: "Invalid token" }`

---

### 3.4 Authorization (Role-Based Access Control)

#### TC-AUTH-014: PASSENGER Access to Booking Endpoint
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- User with role "PASSENGER" logged in

**Test Steps**:
1. POST `/api/booking/initiate` with valid booking request

**Expected Result**:
- HTTP Status: 200 OK (if booking data valid)
- Request processed

---

#### TC-AUTH-015: PASSENGER Denied Access to Admin Endpoint
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- User with role "PASSENGER" logged in

**Test Steps**:
1. GET `/api/admin/users`

**Expected Result**:
- HTTP Status: 403 Forbidden
- Response: `{ success: false, message: "Insufficient permissions" }`

---

#### TC-AUTH-016: ADMIN Access to Admin Endpoint
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- User with role "ADMIN" logged in

**Test Steps**:
1. GET `/api/admin/users`

**Expected Result**:
- HTTP Status: 200 OK
- Returns list of users

---

---

## 4. Search & Route Discovery Test Cases

### 4.1 Search Endpoint Tests

#### TC-SEARCH-001: Search with Valid Origin and Destination
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Route "NYC" → "Boston" exists with 2 schedules
- Both schedules have available seats
- Authenticated user

**Test Steps**:
1. GET `/api/search?origin=NYC&destination=Boston&page=0&size=10`

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, data: [ScheduleDto, ...], totalElements: 2 }`
- Each ScheduleDto includes: `scheduleId`, `departureTime`, `arrivalTime`, `availableSeats`, `basePrice`

**Test Data**: BUS type, departure in future

---

#### TC-SEARCH-002: Search with Type Filter
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Multiple schedules with different types (BUS, RAIL, FLIGHT)

**Test Steps**:
1. GET `/api/search?origin=NYC&destination=Boston&type=RAIL`

**Expected Result**:
- HTTP Status: 200 OK
- Only RAIL type schedules returned

---

#### TC-SEARCH-003: Search with Date Filter
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Schedules on different dates exist

**Test Steps**:
1. GET `/api/search?origin=NYC&destination=Boston&date=2026-03-15`

**Expected Result**:
- HTTP Status: 200 OK
- Only schedules departing on 2026-03-15 returned

---

#### TC-SEARCH-004: Search with Missing Required Parameters
- **Type**: Negative
- **Priority**: P1

**Test Steps**:
1. GET `/api/search?origin=NYC` (missing destination)

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, errors: { destination: "Destination is required" } }`

---

#### TC-SEARCH-005: Search Returns No Results
- **Type**: Negative
- **Priority**: P2

**Preconditions**:
- No route from "Unknown1" to "Unknown2"

**Test Steps**:
1. GET `/api/search?origin=Unknown1&destination=Unknown2`

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, data: [], totalElements: 0 }`

---

#### TC-SEARCH-006: Search with Pagination
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- 25 schedules exist for origin-destination

**Test Steps**:
1. GET `/api/search?origin=NYC&destination=Boston&page=0&size=10`
2. GET `/api/search?origin=NYC&destination=Boston&page=1&size=10`

**Expected Result**:
- Page 0: 10 items, page 1: 10 items (both from same search)
- Each page has `totalElements: 25`

---

### 4.2 Caching Tests

#### TC-SEARCH-007: Repeated Search Uses Cache
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- Redis running, caching enabled
- Cache TTL: 1 hour

**Test Steps**:
1. GET `/api/search?origin=NYC&destination=Boston` (T=0)
2. Wait 5 seconds
3. GET `/api/search?origin=NYC&destination=Boston` (T=5)
4. Verify second request uses cached response

**Expected Result**:
- Both requests return identical results
- Second request has lower latency
- Cache hit verified via logs or metrics

---

#### TC-SEARCH-008: Cache Invalidation on Schedule Update
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- Schedule exists and cached
- User with OPERATOR role

**Test Steps**:
1. Cache schedule search
2. Update schedule pricing (PUT `/api/schedule/{id}`)
3. Search again

**Expected Result**:
- Cache invalidated
- New results reflect updated pricing

---

---

## 5. Inventory Management Test Cases

### 5.1 Seat Hold Tests

#### TC-INVENTORY-001: Hold Available Seats Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Schedule with 10 AVAILABLE seats
- Authenticated user

**Test Steps**:
1. POST `/api/inventory/hold` with payload:
   ```json
   {
     "scheduleId": 1,
     "seatIds": [101, 102, 103],
     "sessionId": "sess_abc123"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, message: "Seats held successfully", holdReference: "..." }`
- Database: 3 seats marked as HELD
- Redis: Hold record created with 5-minute TTL

**Test Data**: Valid seat IDs, unique session ID

---

#### TC-INVENTORY-002: Hold Non-Existent Seat
- **Type**: Negative
- **Priority**: P1

**Test Steps**:
1. POST `/api/inventory/hold` with non-existent seatId [999]

**Expected Result**:
- HTTP Status: 404 Not Found
- Response: `{ success: false, message: "Seat not found" }`

---

#### TC-INVENTORY-003: Hold Already-Held Seat
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Seat 101 already held by another user

**Test Steps**:
1. POST `/api/inventory/hold` with seatId [101]

**Expected Result**:
- HTTP Status: 409 Conflict
- Response: `{ success: false, message: "Seat not available" }`

---

#### TC-INVENTORY-004: Hold Seats from Different Schedules
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Seat 101 from Schedule 1, Seat 201 from Schedule 2

**Test Steps**:
1. POST `/api/inventory/hold` with scheduleId=1 and seatIds=[101, 201]

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "All seats must be from the same schedule" }`

---

### 5.2 Seat Release Tests

#### TC-INVENTORY-005: Release Held Seats Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Seats held with sessionId "sess_abc123"

**Test Steps**:
1. POST `/api/inventory/release` with payload:
   ```json
   {
     "sessionId": "sess_abc123"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, message: "Seats released" }`
- Database: 3 seats marked as AVAILABLE
- Redis: Hold record deleted

---

#### TC-INVENTORY-006: Release Non-Existent Hold
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. POST `/api/inventory/release` with non-existent sessionId

**Expected Result**:
- HTTP Status: 404 Not Found
- Response: `{ success: false, message: "Hold not found" }`

---

### 5.3 Hold Expiration Tests

#### TC-INVENTORY-007: Held Seat Reverts to AVAILABLE After TTL Expiry
- **Type**: Edge Case
- **Priority**: P2

**Preconditions**:
- Hold TTL set to 5 minutes
- Can manipulate time via test utilities (e.g., MockClock)

**Test Steps**:
1. Hold seat at T=0
2. Advance time to T=6 minutes
3. Search for available seats in schedule

**Expected Result**:
- Held seat now shows as AVAILABLE in search results
- Status in database changed from HELD to AVAILABLE

---

---

## 6. Booking Management Test Cases

### 6.1 Create Booking Tests

#### TC-BOOKING-001: Create Booking Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Seats held with valid sessionId
- Authenticated PASSENGER user
- Booking not yet created for this hold

**Test Steps**:
1. POST `/api/booking/initiate` with payload:
   ```json
   {
     "scheduleId": 1,
     "seatIds": [101, 102],
     "holdSessionId": "sess_abc123"
   }
   ```

**Expected Result**:
- HTTP Status: 201 Created
- Response: `{ success: true, data: { bookingId, pnr, status: "INITIATED", totalAmount: 25000 } }`
- Database: Booking created with INITIATED status
- Database: 2 BookingItems created
- PNR: Unique, 6-character alphanumeric

**Test Data**: Valid held seats, user as owner

---

#### TC-BOOKING-002: Create Booking with Expired Hold
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Hold expired (removed from Redis)

**Test Steps**:
1. POST `/api/booking/initiate` with expired holdSessionId

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Hold expired or not found" }`

---

#### TC-BOOKING-003: Create Booking for Another User's Hold
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Hold created by User A
- Attempting to book as User B

**Test Steps**:
1. User B: POST `/api/booking/initiate` with User A's holdSessionId

**Expected Result**:
- HTTP Status: 403 Forbidden
- Response: `{ success: false, message: "Hold does not belong to you" }`

---

#### TC-BOOKING-004: Create Booking with Non-Existent Schedule
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. POST `/api/booking/initiate` with scheduleId=999

**Expected Result**:
- HTTP Status: 404 Not Found
- Response: `{ success: false, message: "Schedule not found" }`

---

### 6.2 Confirm Booking Tests

#### TC-BOOKING-005: Confirm Booking Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Booking in INITIATED status
- Payment with same amount in COMPLETED status
- Authenticated user owns booking

**Test Steps**:
1. POST `/api/booking/confirm` with payload:
   ```json
   {
     "bookingId": 1,
     "paymentId": 5
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, data: { bookingId, status: "CONFIRMED", ... } }`
- Database: Booking.status = CONFIRMED
- Database: 2 Seat.status = BOOKED
- Side Effect: Payment linked to booking

---

#### TC-BOOKING-006: Confirm with Incomplete Payment
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Payment.status = PENDING (not COMPLETED)

**Test Steps**:
1. POST `/api/booking/confirm` with pending paymentId

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Payment not completed" }`

---

#### TC-BOOKING-007: Confirm Already-Confirmed Booking
- **Type**: Negative
- **Priority**: P2

**Preconditions**:
- Booking.status = CONFIRMED

**Test Steps**:
1. POST `/api/booking/confirm` on confirmed booking

**Expected Result**:
- HTTP Status: 409 Conflict
- Response: `{ success: false, message: "Booking already confirmed" }`

---

### 6.3 Cancel Booking Tests

#### TC-BOOKING-008: Cancel Initiated Booking
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Booking.status = INITIATED
- Authenticated user owns booking

**Test Steps**:
1. POST `/api/booking/cancel` with payload:
   ```json
   {
     "bookingId": 1,
     "reason": "Changed plans"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, data: { bookingId, status: "CANCELLED" } }`
- Database: Booking.status = CANCELLED
- Database: Seats reverted to AVAILABLE
- No refund created (no payment)

---

#### TC-BOOKING-009: Cancel Confirmed Booking Within 24 Hours
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Booking.status = CONFIRMED
- Created < 24 hours ago
- Payment completed and refundable

**Test Steps**:
1. POST `/api/booking/cancel` with reason

**Expected Result**:
- HTTP Status: 200 OK
- Booking: Cancelled
- Refund: Created with 100% amount
- Seats: Reverted to AVAILABLE

---

#### TC-BOOKING-010: Cancel Confirmed Booking After 24 Hours
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- Booking.status = CONFIRMED
- Created > 24 hours ago

**Test Steps**:
1. POST `/api/booking/cancel` with reason

**Expected Result**:
- HTTP Status: 200 OK
- Booking: Cancelled
- Refund: Created with 50% amount (50% cancellation fee)
- Seats: Reverted to AVAILABLE

---

### 6.4 List Bookings Tests

#### TC-BOOKING-011: List User's Bookings
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- User has 5 bookings
- Authenticated user

**Test Steps**:
1. GET `/api/booking/list?page=0&size=10`

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ data: [BookingDto, ...], totalElements: 5 }`
- Only user's own bookings returned

---

#### TC-BOOKING-012: Filter Bookings by Status
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- User has 3 CONFIRMED and 2 CANCELLED bookings

**Test Steps**:
1. GET `/api/booking/list?status=CONFIRMED`

**Expected Result**:
- HTTP Status: 200 OK
- Response: 3 CONFIRMED bookings returned

---

---

## 7. Payment Processing Test Cases

### 7.1 Create Payment Intent Tests

#### TC-PAYMENT-001: Create Payment Intent Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Booking in INITIATED status with amount 25000
- Authenticated user owns booking

**Test Steps**:
1. POST `/api/payment/create-intent` with payload:
   ```json
   {
     "bookingId": 1,
     "amount": 25000
   }
   ```

**Expected Result**:
- HTTP Status: 201 Created
- Response: `{ success: true, data: { paymentId, status: "PENDING", externalReference: "stripe_pi_..." } }`
- Database: Payment created with PENDING status
- Gateway: Intent created (stub)

**Test Data**: Amount matches booking total

---

#### TC-PAYMENT-002: Create Payment with Wrong Amount
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Booking amount = 25000

**Test Steps**:
1. POST `/api/payment/create-intent` with amount 20000

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Amount does not match booking total" }`

---

#### TC-PAYMENT-003: Create Payment for Non-Existent Booking
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. POST `/api/payment/create-intent` with bookingId=999

**Expected Result**:
- HTTP Status: 404 Not Found
- Response: `{ success: false, message: "Booking not found" }`

---

### 7.2 Confirm Payment Tests

#### TC-PAYMENT-004: Confirm Payment Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Payment.status = PENDING
- Valid transaction ID from gateway

**Test Steps**:
1. POST `/api/payment/confirm` with payload:
   ```json
   {
     "paymentId": 5,
     "transactionId": "stripe_txn_12345",
     "status": "completed"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true, data: { paymentId, status: "COMPLETED", transactionId: "stripe_txn_12345" } }`
- Database: Payment.status = COMPLETED
- Event: Booking confirmation triggered (async)

---

#### TC-PAYMENT-005: Confirm Payment with Invalid Transaction
- **Type**: Negative
- **Priority**: P1

**Test Steps**:
1. POST `/api/payment/confirm` with invalid transactionId

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Transaction verification failed" }`
- Database: Payment remains PENDING

---

#### TC-PAYMENT-006: Confirm Already-Completed Payment
- **Type**: Negative
- **Priority**: P2

**Preconditions**:
- Payment.status = COMPLETED

**Test Steps**:
1. POST `/api/payment/confirm` with same paymentId

**Expected Result**:
- HTTP Status: 409 Conflict
- Response: `{ success: false, message: "Payment already completed" }`

---

### 7.3 Webhook Tests

#### TC-PAYMENT-007: Webhook Payment Confirmation
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Payment exists with valid webhook signature
- Idempotency key prevents duplicate processing

**Test Steps**:
1. POST `/api/payment/webhook` (from payment gateway) with payload:
   ```json
   {
     "paymentId": 5,
     "status": "completed",
     "transactionId": "stripe_txn_12345",
     "signature": "hmac_sha256_hash"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- Response: `{ success: true }`
- Database: Payment.status = COMPLETED
- Booking: Automatically confirmed (if not already)

---

#### TC-PAYMENT-008: Webhook with Invalid Signature
- **Type**: Negative
- **Priority**: P1

**Test Steps**:
1. POST `/api/payment/webhook` with tampered signature

**Expected Result**:
- HTTP Status: 401 Unauthorized
- Response: `{ success: false, message: "Invalid webhook signature" }`
- Database: No changes

---

#### TC-PAYMENT-009: Duplicate Webhook (Idempotency)
- **Type**: Edge Case
- **Priority**: P2

**Preconditions**:
- Webhook already processed with idempotency key "idem_abc123"

**Test Steps**:
1. POST `/api/payment/webhook` with same idempotency key
2. Webhook should be processed again (same result)

**Expected Result**:
- First: HTTP 200, payment completed
- Second: HTTP 200, same result (idempotent)
- Database: Only one Payment update

---

### 7.4 Refund Tests

#### TC-PAYMENT-010: Create Refund Successfully
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Payment.status = COMPLETED with amount 25000

**Test Steps**:
1. POST `/api/payment/refund` with payload:
   ```json
   {
     "paymentId": 5,
     "amount": 25000,
     "reason": "CUSTOMER_REQUEST"
   }
   ```

**Expected Result**:
- HTTP Status: 201 Created
- Response: `{ success: true, data: { refundId, status: "PENDING", amount: 25000 } }`
- Database: Refund created with PENDING status
- Gateway: Refund initiated

---

#### TC-PAYMENT-011: Create Partial Refund
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Payment amount = 25000

**Test Steps**:
1. POST `/api/payment/refund` with amount 12500

**Expected Result**:
- HTTP Status: 201 Created
- Refund created with 12500
- Payment not marked as REFUNDED (still COMPLETED)
- Second refund can be created for remaining amount

---

#### TC-PAYMENT-012: Refund Exceeds Payment Amount
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- Payment amount = 25000

**Test Steps**:
1. POST `/api/payment/refund` with amount 30000

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Refund amount exceeds payment amount" }`

---

#### TC-PAYMENT-013: Refund on Non-Completed Payment
- **Type**: Negative
- **Priority**: P2

**Preconditions**:
- Payment.status = FAILED

**Test Steps**:
1. POST `/api/payment/refund` for failed payment

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Only completed payments can be refunded" }`

---

---

## 8. Refunds Management Test Cases

### 8.1 Refund Policy Tests

#### TC-REFUND-001: Full Refund for Booking Cancelled Within 24h
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Booking confirmed 10 hours ago
- Policy: Full refund within 24h

**Test Steps**:
1. Cancel booking via POST `/api/booking/cancel`

**Expected Result**:
- Refund amount = 100% of booking total
- Refund created and marked PENDING

**Expected Result**:
- Refund amount = 100% of booking total
- Refund in PENDING status

---

#### TC-REFUND-002: Partial Refund for Booking Cancelled After 24h
- **Type**: Positive
- **Priority**: P0

**Preconditions**:
- Booking confirmed 25 hours ago
- Policy: 50% cancellation fee after 24h, refund 50%

**Test Steps**:
1. Cancel booking

**Expected Result**:
- Refund amount = 50% of booking total
- Refund created with PENDING status

---

#### TC-REFUND-003: No Refund for Booking with Active Ticket
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- Booking confirmed for travel within 24 hours
- Policy: No refund for active/near-travel bookings

**Test Steps**:
1. Attempt to cancel booking

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response: `{ success: false, message: "Cannot refund booking within 24 hours of departure" }`

---

### 8.2 Refund Status Tracking Tests

#### TC-REFUND-004: Refund Status Transitions
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- Refund created with PENDING status

**Test Steps**:
1. T=0: Refund created (PENDING)
2. T=5s: Webhook from gateway confirms refund (COMPLETED)

**Expected Result**:
- Initial: PENDING
- After webhook: COMPLETED
- Booking payment: Updated to REFUNDED

---

#### TC-REFUND-005: Refund Failure Retry Logic
- **Type**: Edge Case
- **Priority**: P2

**Preconditions**:
- Refund.status = PENDING
- Gateway returns transient error

**Test Steps**:
1. Retry refund processing (max 3 retries with exponential backoff)

**Expected Result**:
- After retry 1 (5s delay): PENDING
- After retry 2 (10s delay): PENDING
- After retry 3 (20s delay): FAILED (mark as failed)
- Log shows retry attempts

---

---

## 9. Admin & User Management Test Cases

### 9.1 Admin User Management Tests

#### TC-ADMIN-001: Admin List All Users
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Authenticated ADMIN user
- 50 users exist in database

**Test Steps**:
1. GET `/api/admin/users?page=0&size=20`

**Expected Result**:
- HTTP Status: 200 OK
- Response: Paginated list of 20 users, totalElements: 50

---

#### TC-ADMIN-002: Admin Create User
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Authenticated ADMIN user

**Test Steps**:
1. POST `/api/admin/users` with payload:
   ```json
   {
     "username": "newoperator",
     "email": "operator@example.com",
     "password": "SecurePass123",
     "role": "OPERATOR"
   }
   ```

**Expected Result**:
- HTTP Status: 201 Created
- User created with OPERATOR role
- Audit log: "CREATE_USER" action recorded

---

#### TC-ADMIN-003: Admin Update User Role
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- User exists with PASSENGER role

**Test Steps**:
1. PUT `/api/admin/users/{userId}` with payload:
   ```json
   {
     "role": "OPERATOR"
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- User.role updated to OPERATOR
- Audit log: "UPDATE_USER" recorded with from/to values

---

### 9.2 User Disable/Enable Tests

#### TC-ADMIN-004: Admin Disable User Account
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- User exists with enabled=true

**Test Steps**:
1. PUT `/api/admin/users/{userId}/status` with payload:
   ```json
   {
     "enabled": false
   }
   ```

**Expected Result**:
- HTTP Status: 200 OK
- User.enabled = false
- User cannot login on next attempt
- Existing JWT remains valid (checked on request)

---

#### TC-ADMIN-005: Login Attempt with Disabled Account
- **Type**: Negative
- **Priority**: P1

**Preconditions**:
- User account disabled

**Test Steps**:
1. POST `/api/auth/login` with disabled user credentials

**Expected Result**:
- HTTP Status: 403 Forbidden
- Response: `{ success: false, message: "User account disabled" }`

---

### 9.3 Audit Log Tests

#### TC-ADMIN-006: View Audit Logs
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Multiple audit log entries exist
- Authenticated ADMIN user

**Test Steps**:
1. GET `/api/admin/audit-logs?action=CREATE_BOOKING&page=0&size=10`

**Expected Result**:
- HTTP Status: 200 OK
- Response: Paginated list of audit logs filtered by action
- Each log shows: `userId`, `action`, `entityType`, `entityId`, `timestamp`, `result`

---

#### TC-ADMIN-007: Audit Log Retention
- **Type**: Edge Case
- **Priority**: P3

**Preconditions**:
- Audit log retention policy: 1 year
- Log entry created > 1 year ago

**Test Steps**:
1. Run cleanup job (daily scheduled)
2. Verify old logs deleted

**Expected Result**:
- Logs older than 1 year deleted
- Recent logs retained
- Job logs success/count

---

---

## 10. Common Features Test Cases

### 10.1 API Response Wrapper Tests

#### TC-COMMON-001: Success Response Format
- **Type**: Positive
- **Priority**: P2

**Test Steps**:
1. Call any successful endpoint

**Expected Result**:
- Response includes required fields:
  ```json
  {
    "success": true,
    "data": { ... },
    "message": "Operation successful",
    "timestamp": "2026-03-03T10:30:00Z",
    "correlationId": "uuid"
  }
  ```

---

#### TC-COMMON-002: Error Response Format
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. Call endpoint with invalid data

**Expected Result**:
- Response includes:
  ```json
  {
    "success": false,
    "message": "Validation failed",
    "timestamp": "2026-03-03T10:30:00Z",
    "correlationId": "uuid",
    "errors": {
      "field1": "error message"
    }
  }
  ```

---

### 10.2 Correlation ID Tests

#### TC-COMMON-003: Correlation ID Propagation
- **Type**: Positive
- **Priority**: P2

**Test Steps**:
1. Make request with custom `X-Correlation-ID` header
2. Verify correlation ID in response headers and logs

**Expected Result**:
- Response includes `X-Correlation-ID` header with provided value
- Logs show same correlation ID
- Database audit logs include correlation ID

---

#### TC-COMMON-004: Correlation ID Generation
- **Type**: Positive
- **Priority**: P2

**Test Steps**:
1. Make request without `X-Correlation-ID` header
2. Verify auto-generated correlation ID

**Expected Result**:
- Response includes generated UUID in `X-Correlation-ID`
- UUID format valid
- Unique per request

---

### 10.3 Idempotency Tests

#### TC-COMMON-005: Idempotent Create Request
- **Type**: Positive
- **Priority**: P1

**Preconditions**:
- Idempotency-Key: "idem_abc123"

**Test Steps**:
1. POST `/api/booking/initiate` with idempotency key
2. POST same request with same idempotency key (within 24h)

**Expected Result**:
- First: HTTP 201, booking created
- Second: HTTP 201, same booking returned (from cache)
- Database: Only one booking created

---

#### TC-COMMON-006: Non-Idempotent Request
- **Type**: Positive
- **Priority**: P2

**Test Steps**:
1. DELETE `/api/booking/{id}` (should have idempotency key?)
2. Verify deletion not cached

**Expected Result**:
- Request processed normally without idempotency check
- Second delete returns 404 (resource deleted)

---

### 10.4 Rate Limiting Tests

#### TC-COMMON-007: Rate Limit Enforcement
- **Type**: Edge Case
- **Priority**: P1

**Preconditions**:
- Rate limit: 100 requests/hour per IP
- Test with simulated time

**Test Steps**:
1. Make 100 requests from IP 192.168.1.1
2. Make 101st request within same hour

**Expected Result**:
- Requests 1-100: HTTP 200/2xx (success)
- Request 101: HTTP 429 Too Many Requests
- Response header: `Retry-After: 3600` (seconds until reset)

---

#### TC-COMMON-008: Rate Limit Reset
- **Type**: Edge Case
- **Priority**: P2

**Preconditions**:
- Rate limited (hit 429)

**Test Steps**:
1. Wait 1 hour (or simulate time advance)
2. Make request from same IP

**Expected Result**:
- HTTP 200 OK
- Request counter reset
- Can make 100 more requests in new hour

---

#### TC-COMMON-009: Rate Limit Per IP
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- IP1: 100 requests made (at limit)
- IP2: Fresh IP

**Test Steps**:
1. Make request from IP1: Returns 429
2. Make request from IP2: Returns 200

**Expected Result**:
- Rate limit tracked per IP
- IP2 not affected by IP1 limit

---

#### TC-COMMON-010: Public Endpoints Exempt from Rate Limit
- **Type**: Positive
- **Priority**: P2

**Preconditions**:
- Public endpoints: `/api/auth/register`, `/api/auth/login`

**Test Steps**:
1. Make 150 requests to `/api/auth/register` from same IP

**Expected Result**:
- All 150 requests succeed (no 429)
- Public endpoints not rate-limited

---

### 10.5 Error Handling Tests

#### TC-COMMON-011: 400 Bad Request
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. POST `/api/booking/initiate` with missing required field

**Expected Result**:
- HTTP Status: 400 Bad Request
- Response includes field error details

---

#### TC-COMMON-012: 401 Unauthorized
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. GET `/api/booking/list` without JWT

**Expected Result**:
- HTTP Status: 401 Unauthorized

---

#### TC-COMMON-013: 403 Forbidden
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. PASSENGER user: GET `/api/admin/users`

**Expected Result**:
- HTTP Status: 403 Forbidden

---

#### TC-COMMON-014: 404 Not Found
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. GET `/api/booking/999` (non-existent booking)

**Expected Result**:
- HTTP Status: 404 Not Found

---

#### TC-COMMON-015: 500 Server Error
- **Type**: Negative
- **Priority**: P2

**Test Steps**:
1. Trigger unhandled exception (e.g., database connection failure)

**Expected Result**:
- HTTP Status: 500 Internal Server Error
- Response includes error ID for support tracking
- Logged with full stack trace

---

---

## 11. Integration Test Cases

### 11.1 End-to-End Booking Workflow

#### TC-INTEGRATION-001: Complete Booking Workflow
- **Type**: Integration
- **Priority**: P0

**Preconditions**:
- System fully initialized

**Test Steps**:
1. Register user → TC-AUTH-001
2. Login → TC-AUTH-006
3. Search routes → TC-SEARCH-001
4. Hold seats → TC-INVENTORY-001
5. Create booking → TC-BOOKING-001
6. Create payment intent → TC-PAYMENT-001
7. Confirm payment → TC-PAYMENT-004
8. Confirm booking → TC-BOOKING-005
9. Verify booking confirmed and seats booked

**Expected Result**:
- All steps succeed
- Booking in CONFIRMED status
- Seats in BOOKED status
- Payment in COMPLETED status
- Audit logs record all actions

---

#### TC-INTEGRATION-002: Booking Cancellation with Refund
- **Type**: Integration
- **Priority**: P0

**Preconditions**:
- Completed booking (from TC-INTEGRATION-001)
- Booking < 24 hours old

**Test Steps**:
1. Cancel booking → Cancel Booking endpoint
2. Verify refund created → Check Refund entity
3. Wait for webhook from payment gateway
4. Verify refund completed
5. Verify user payload

**Expected Result**:
- Booking: CANCELLED
- Refund: COMPLETED
- Seats: AVAILABLE
- User refunded

---

### 11.2 Concurrent Access Tests

#### TC-INTEGRATION-003: Concurrent Seat Hold Requests
- **Type**: Integration
- **Priority**: P1

**Preconditions**:
- Seat 101 has status AVAILABLE
- 2 concurrent requests to hold same seat

**Test Steps**:
1. User A: Hold seat 101 (T=0)
2. User B: Hold seat 101 (T=0, concurrent)

**Expected Result**:
- First request: Success, seat becomes HELD
- Second request: Conflict (409), seat already held
- Only one user holds seat

---

#### TC-INTEGRATION-004: Double Booking Prevention
- **Type**: Integration
- **Priority**: P1

**Preconditions**:
- Seat held and booking in progress
- 2 concurrent confirmation requests

**Test Steps**:
1. User A: Confirm booking with seat 101
2. User B: Confirm booking with seat 101 (concurrent)

**Expected Result**:
- First: Success, seat booked
- Second: Conflict (409), seat no longer available/held

---

### 11.3 Multi-User Scenario Tests

#### TC-INTEGRATION-005: Multiple Users Search and Book Same Schedule
- **Type**: Integration
- **Priority**: P2

**Preconditions**:
- Schedule with 10 available seats
- 3 users perform search and booking

**Test Steps**:
1. User A: Search (see all 10 seats)
2. User B: Search (see all 10 seats)
3. User A: Hold seats 1-3
4. User B: Search (see 7 available + 3 held)
5. User B: Hold seats 4-6
6. User A: Book seats 1-3
7. User B: Book seats 4-6

**Expected Result**:
- All searches return correct available count
- Both users can book simultaneously
- No double-booking

---

---

## 12. Performance & Load Test Cases

### 12.1 Response Time Tests

#### TC-PERF-001: Search Response Time < 500ms
- **Type**: Performance
- **Priority**: P2

**Preconditions**:
- Database with 100K+ schedules
- Search cache enabled

**Test Steps**:
1. Execute search query
2. Measure response time

**Expected Result**:
- Response time: < 500ms (95th percentile)
- Both cached and uncached queries measured

---

#### TC-PERF-002: Booking Creation Under Load
- **Type**: Performance
- **Priority**: P2

**Preconditions**:
- 100+ concurrent users booking

**Test Steps**:
1. Load test with JMeter: 100 concurrent booking creation requests
2. Measure throughput and response times

**Expected Result**:
- Throughput: > 10 bookings/second
- 95th percentile response: < 1000ms
- No database deadlocks

---

### 12.2 Load Tests

#### TC-PERF-003: Peak Load: 1000 Concurrent Users
- **Type**: Load
- **Priority**: P3

**Test Steps**:
1. Simulate 1000 concurrent users
2. Mix of search (50%), booking (30%), payment (20%)

**Expected Result**:
- Application remains responsive
- No connection pool exhaustion
- Graceful degradation if limits reached
- Monitored memory usage < 80% heap

---

### 12.3 Scalability Tests

#### TC-PERF-004: Horizontal Scaling
- **Type**: Scalability
- **Priority**: P3

**Preconditions**:
- 2 application instances behind load balancer

**Test Steps**:
1. Generate load: 500 req/s
2. Both instances running
3. Disable instance 1
4. Verify traffic routed to instance 2

**Expected Result**:
- Load balanced across instances
- Failover works smoothly
- No request loss

---

---

## 13. Security Test Cases

### 13.1 Authentication Security Tests

#### TC-SEC-001: Password Hashing
- **Type**: Security
- **Priority**: P0

**Test Steps**:
1. Register user with password "TestPass123"
2. Verify stored password in database

**Expected Result**:
- Password NOT stored in plaintext
- Password hashed with bcrypt (salt rounds ≥ 10)
- Hash starts with `$2b$` or `$2y$` prefix

---

#### TC-SEC-002: JWT Secret Minimum Length
- **Type**: Security
- **Priority**: P0

**Preconditions**:
- JWT_SECRET < 32 bytes (weak secret)

**Test Steps**:
1. Application startup with weak secret
2. Check logs

**Expected Result**:
- WARNING logged: "JWT Secret too weak, minimum 32-bytes recommended"
- System falls back to generated key (with warning)
- Or rejects startup (stricter policy)

---

#### TC-SEC-003: SQL Injection Prevention
- **Type**: Security
- **Priority**: P0

**Test Steps**:
1. Search endpoint: `?origin=NYC' OR '1'='1`
2. Verify query safely handles input

**Expected Result**:
- No SQL injection vulnerability
- Query returns no results (no matching origin)
- Logs suspicious input (optional)

---

#### TC-SEC-004: XSS Prevention
- **Type**: Security
- **Priority**: P0

**Test Steps**:
1. Create booking with passenger name: `<script>alert('XSS')</script>`
2. Retrieve booking details
3. Verify script not executed in response

**Expected Result**:
- Script tags escaped or removed
- Response safe for rendering in HTML

---

### 13.2 Authorization Security Tests

#### TC-SEC-005: Resource Ownership Validation
- **Type**: Security
- **Priority**: P0

**Preconditions**:
- User A creates booking
- User B has valid JWT

**Test Steps**:
1. User B: GET `/api/booking/{User A's booking ID}`

**Expected Result**:
- HTTP Status: 403 Forbidden
- Response: `{ success: false, message: "Access denied" }`

---

#### TC-SEC-006: Role-Based Access Control
- **Type**: Security
- **Priority**: P0

**Test Steps**:
1. PASSENGER JWT: POST `/api/admin/users`
2. Verify endpoint rejects request

**Expected Result**:
- HTTP Status: 403 Forbidden
- Request denied

---

### 13.3 HTTPS & Transport Security

#### TC-SEC-007: HTTPS Enforcement
- **Type**: Security
- **Priority**: P1

**Preconditions**:
- HTTPS enabled in production

**Test Steps**:
1. Attempt HTTP request to API
2. Verify redirect or rejection

**Expected Result**:
- HTTP request redirected to HTTPS (301/302)
- Or rejected if strict mode

---

#### TC-SEC-008: Sensitive Header Handling
- **Type**: Security
- **Priority**: P1

**Test Steps**:
1. Make request with sensitive data
2. Verify response headers

**Expected Result**:
- No sensitive data in response headers
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security` (HTTPS)

---

### 13.4 Data Protection Tests

#### TC-SEC-009: Sensitive Data Logging
- **Type**: Security
- **Priority**: P0

**Test Steps**:
1. Register user with password "SecurePass123"
2. Check application logs

**Expected Result**:
- Password NOT logged
- JWT tokens NOT logged
- Only sanitized data logged

---

#### TC-SEC-010: PII in Audit Logs
- **Type**: Security
- **Priority**: P1

**Test Steps**:
1. Perform sensitive action (payment confirmation)
2. Check audit logs in database

**Expected Result**:
- PII (emails, names) masked or excluded
- Only necessary identifiers logged
- Payment amounts not logged

---

---

## Test Execution Strategy

### Environment Setup
1. **Local Dev**: H2 in-memory, mocked external services
2. **CI/CD**: Docker containers, PostgreSQL test DB
3. **Staging**: Full production-like environment

### Test Phases

| Phase | Tests | Tools | Timing |
|-------|-------|-------|--------|
| **Unit** | TC-AUTH-001 to TCP-COMMON-015 | JUnit 5, Mockito | Per commit |
| **Integration** | TC-INTEGRATION-* | Spring Boot Test | Daily |
| **Performance** | TC-PERF-* | JMeter, wrk | Weekly |
| **Security** | TC-SEC-* | OWASP ZAP, manual | Monthly |
| **UAT** | Subset of all | Manual | Before release |

### Coverage Targets
- **Line Coverage**: ≥80%
- **Branch Coverage**: ≥75%
- **Critical Paths**: 100% (auth, payment, booking)

### Reporting
- JUnit reports: `target/surefire-reports/`
- Coverage reports: `target/site/jacoco/`
- Performance benchmarks: Prometheus/Grafana
- Security scan results: OWASP/SonarQube

---

## Test Data Management

### Test Data Cleanup
- Transactional tests: Auto-rollback
- Integration tests: Manual cleanup in `@After` / `@AfterEach`
- Performance tests: Dedicated test DB (reset between runs)

### Test Data Fixtures

**Example Factory Method**:
```java
@Component
public class TestDataFactory {
  public User createUser(String username, Role role) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username + "@test.com");
    user.setPassword(bCryptPasswordEncoder.encode("TestPass123"));
    user.setRole(role);
    return userRepository.save(user);
  }
  
  public Schedule createSchedule(Route route, LocalDateTime departure) {
    Schedule schedule = new Schedule();
    schedule.setRoute(route);
    schedule.setDepartureTime(departure);
    schedule.setArrivalTime(departure.plusHours(2));
    schedule.setBasePrice(100.0);
    return scheduleRepository.save(schedule);
  }
}
```

---

## Known Issues & Blocked Tests

| Test ID | Issue | Status | ETA |
|---------|-------|--------|-----|
| TC-PERF-003 | Load test requires K8s cluster | Blocked | Q2-2026 |
| TC-SEC-008 | HTTPS setup pending | Blocked | Prod deployment |

---

## References

- [JUnit 5 Documentation](https://junit.org/junit5/docs/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [JMeter Load Testing](https://jmeter.apache.org/usermanual/index.html)

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Test Owner**: QA Team
