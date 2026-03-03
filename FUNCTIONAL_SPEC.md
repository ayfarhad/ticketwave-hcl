# Functional Specification - TicketWave Backend

## 1. System Overview

**TicketWave** is a comprehensive ticket booking and management system designed for transportation providers (bus, rail, airline). The platform enables customers to search available routes, hold seats, create bookings, process payments, and manage refunds, while providing operators and administrators with tools to manage inventory, monitor bookings, and generate audit logs.

### 1.1 Core Objectives
- Facilitate seamless customer booking experience
- Manage seat inventory with real-time availability  
- Process payments securely with third-party provider integration
- Provide audit trails and compliance logging
- Deliver high-performance API with rate limiting and caching
- Support multi-user access with role-based authorization

### 1.2 Key Stakeholders
- **Customers/Passengers**: Search, book, pay, manage tickets
- **Operators**: Manage routes, schedules, seats, pricing
- **Admins**: User management, audit monitoring, system configuration
- **Payment Providers**: External payment processing integration

---

## 2. Functional Subsystems

### 2.1 Authentication & Authorization

#### 2.1.1 User Registration
- **Endpoint**: `POST /api/auth/register`
- **Request**:
  ```json
  {
    "username": "string",
    "email": "string",
    "password": "string"
  }
  ```
- **Response**: `ApiResponse<UserDto>` with JWT token
- **Validation**:
  - Username: non-empty, unique
  - Email: valid format, unique
  - Password: minimum 8 characters (configurable)
- **Side Effects**: User entity created in database, default role PASSENGER assigned

#### 2.1.2 User Login
- **Endpoint**: `POST /api/auth/login`
- **Request**:
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response**: JWT token with claims (userId, username, role, expiresAt)
- **Token Properties**:
  - Algorithm: HS256
  - Expiration: 24 hours (configurable)
  - Signature Key: Environment variable `JWT_SECRET` (minimum 32 bytes recommended)
  
#### 2.1.3 Authorization & Roles
- **Roles**:
  - `PASSENGER`: Default role; can search, book, view own bookings
  - `OPERATOR`: Can manage routes, schedules, pricing
  - `ADMIN`: Full system access, user management, audit view
  
- **JWT Filter** (`JwtFilter.java`):
  - Validates token signature and expiration
  - Extracts claims and populates Spring Security context
  - Applies to all endpoints except `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
  
- **Authorization**: Method-level via `@PreAuthorize` annotations

---

### 2.2 Search & Route Discovery

#### 2.2.1 Search Functionality
- **Endpoint**: `GET /api/search?origin=X&destination=Y&date=YYYY-MM-DD&type=BUS&page=0&size=20`
- **Query Parameters**:
  - `origin`: Route origin city/code (required)
  - `destination`: Route destination city/code (required)
  - `date`: Departure date ISO format (optional, filters by date range)
  - `type`: Transport type (BUS, RAIL, FLIGHT) (optional)
  - `page`, `size`: Pagination parameters
  
- **Response**:
  ```json
  {
    "success": true,
    "data": [
      {
        "scheduleId": "long",
        "routeType": "string",
        "departureTime": "ISO8601 timestamp",
        "arrivalTime": "ISO8601 timestamp",
        "availableSeats": "int",
        "basePrice": "double",
        "origin": "string",
        "destination": "string"
      }
    ],
    "totalElements": "int",
    "message": "Schedules found"
  }
  ```

#### 2.2.2 Caching Strategy
- **Technology**: Spring Cache (Redis backend)
- **Key Format**: `schedules::search:{origin}:{destination}:{date}:{type}`
- **TTL**: 1 hour (configurable via `application.yml`)
- **Invalidation**: Manual on schedule creation/update; scheduled daily refresh
- **Cache Warming**: Optional background job to pre-cache popular routes

#### 2.2.3 Available Seat Calculation
- **Logic**: 
  1. Query seats by schedule_id with status='AVAILABLE' or 'HELD' (held seats expire)
  2. Exclude held seats with TTL < current_time
  3. Count remaining seats
  4. If count < 1, schedule marked unavailable
  
---

### 2.3 Inventory Management

#### 2.3.1 Routes
- **Entity**: `Route.java`
- **Attributes**:
  - `routeId`: Unique identifier
  - `origin`: Source city/code
  - `destination`: Destination city/code
  - `transportType`: BUS, RAIL, FLIGHT
  - `createdAt`: Timestamp
  
- **Operations**:
  - Create route (OPERATOR/ADMIN only)
  - List all routes (public)
  - Update route (OPERATOR/ADMIN only)
  - Soft delete via status flag

#### 2.3.2 Schedules
- **Entity**: `Schedule.java`
- **Attributes**:
  - `scheduleId`: Primary key
  - `routeId`: Foreign key to Route
  - `departureTime`: ISO8601 timestamp
  - `arrivalTime`: ISO8601 timestamp
  - `basePrice`: Base fare for 1 passenger
  - `createdAt`, `updatedAt`
  
- **Operations**:
  - Create schedule from route
  - List schedules by route or date range
  - Update departure/arrival times and pricing
  - Publish schedule (makes available for booking)

#### 2.3.3 Seats
- **Entity**: `Seat.java`
- **Attributes**:
  - `seatId`: Unique identifier
  - `scheduleId`: Foreign key to Schedule
  - `seatNumber`: Display number (e.g., "12A", "1F")
  - `class`: ECONOMY, BUSINESS, FIRST
  - `status`: AVAILABLE | HELD | BOOKED | BLOCKED
  - `price`: Override price (if null, uses schedule basePrice)
  
- **Status Transitions**:
  ```
  AVAILABLE --[hold]--> HELD --[confirm]--> BOOKED
                     --[release]--> AVAILABLE
                     --[timeout]--> AVAILABLE
  ```
  
- **Hold Management**:
  - Held seats stored in Redis with session key
  - TTL: 5 minutes (configurable)
  - On expiry, automatically transitioned to AVAILABLE
  - On booking confirmation, transitioned to BOOKED
  - On booking cancellation, transitioned to AVAILABLE

#### 2.3.4 Seat Hold Endpoint
- **Endpoint**: `POST /api/inventory/hold`
- **Request**:
  ```json
  {
    "scheduleId": "long",
    "seatIds": ["long"],
    "sessionId": "string"
  }
  ```
- **Response**: Success boolean, hold reference
- **Logic**:
  1. Verify seats exist and are AVAILABLE
  2. Atomically transition to HELD
  3. Store hold record in Redis with sessionId key
  4. Return hold confirmation

#### 2.3.5 Release Hold Endpoint
- **Endpoint**: `POST /api/inventory/release`
- **Request**:
  ```json
  {
    "sessionId": "string"
  }
  ```
- **Response**: Success boolean
- **Logic**:
  1. Lookup held seats by sessionId
  2. Transition all to AVAILABLE
  3. Delete Redis entry

---

### 2.4 Booking Management

#### 2.4.1 Booking Entity
- **Entity**: `Booking.java`
- **Attributes**:
  - `bookingId`: Unique identifier
  - `userId`: Passenger user ID
  - `pnr`: Alphanumeric confirmation number (PNR generator)
  - `status`: INITIATED | CONFIRMED | CANCELLED | REFUNDED
  - `totalAmount`: Sum of all booking items
  - `createdAt`, `updatedAt`
  
- **PNR Generation** (`PnrGenerator.java`):
  - Format: 6 alphanumeric characters (uppercase + digits)
  - Uniqueness: Assured via database constraints
  - Example: "A1B2C3"

#### 2.4.2 Booking Items
- **Entity**: `BookingItem.java`
- **Attributes**:
  - `itemId`: Primary key
  - `bookingId`: Foreign key to Booking
  - `seatId`: Foreign key to Seat
  - `price`: Price at time of booking
  - `passenger`: Passenger name (for multi-passenger bookings)
  
- **Purpose**: Track individual seats within a booking; supports group bookings

#### 2.4.3 Initiate Booking (Endpoint: POST /api/booking/initiate)
- **Request**:
  ```json
  {
    "scheduleId": "long",
    "seatIds": ["long"],
    "holdSessionId": "string"
  }
  ```
- **Response**: `ApiResponse<BookingDto>` with booking ID and PNR
- **Business Logic**:
  1. Verify hold record exists and is not expired
  2. Verify all seats are in HELD state
  3. Create Booking entity with status INITIATED
  4. Create BookingItems for each seat
  5. Set total amount = sum of seat prices
  6. Generate PNR
  7. Return booking details
  
- **Preconditions**:
  - User authenticated and has PASSENGER role
  - Hold must exist and belong to current user session
  - All seats must be from same schedule
  
- **Error Scenarios**:
  - Seat not held: HTTP 400
  - Hold expired: HTTP 400
  - Seats from multiple schedules: HTTP 400
  - User not authenticated: HTTP 401

#### 2.4.4 Confirm Booking (Endpoint: POST /api/booking/confirm)
- **Request**:
  ```json
  {
    "bookingId": "long",
    "paymentId": "long"
  }
  ```
- **Response**: `ApiResponse<BookingDto>` with updated status
- **Business Logic**:
  1. Retrieve booking by ID
  2. Verify payment is COMPLETED
  3. Transition booking status to CONFIRMED
  4. For each booking item:
     - Transition seat status to BOOKED
  5. Persist changes (transactional)
  6. Return booking details
  
- **Preconditions**:
  - Booking exists and belongs to current user
  - Booking status is INITIATED
  - Payment is confirmed
  
- **Error Scenarios**:
  - Booking not found: HTTP 404
  - Payment not completed: HTTP 400
  - Booking already confirmed: HTTP 400
  - Unauthorized access: HTTP 403

#### 2.4.5 Cancel Booking (Endpoint: POST /api/booking/cancel)
- **Request**:
  ```json
  {
    "bookingId": "long",
    "reason": "string"
  }
  ```
- **Response**: `ApiResponse<BookingDto>` with new status
- **Business Logic**:
  1. Retrieve booking
  2. If status is CONFIRMED, initiate refund process
  3. Transition booking status to CANCELLED
  4. Release all seats to AVAILABLE
  5. Trigger refund if applicable
  
- **Preconditions**:
  - Booking exists and belongs to user
  - Status is INITIATED or CONFIRMED
  - Cancellation allowed (within 24 hours for confirmed, anytime for initiated)

#### 2.4.6 List Bookings (Endpoint: GET /api/booking/list)
- **Query Parameters**: `page`, `size`, `status`
- **Response**: Paginated list of user's bookings
- **Authorization**: User can only view own bookings; ADMIN can view all

---

### 2.5 Payment Processing

#### 2.5.1 Payment Entity
- **Entity**: `Payment.java`
- **Attributes**:
  - `paymentId`: Unique identifier
  - `bookingId`: Reference to booking
  - `amount`: Payment amount in base currency (e.g., cents)
  - `status`: PENDING | COMPLETED | FAILED | REFUNDED
  - `externalReference`: Third-party payment gateway reference
  - `paymentMethod`: CREDIT_CARD, DEBIT_CARD, UPI, WALLET
  - `createdAt`, `updatedAt`

#### 2.5.2 Create Payment Intent (Endpoint: POST /api/payment/create-intent)
- **Request**:
  ```json
  {
    "bookingId": "long",
    "amount": "double"
  }
  ```
- **Response**: `ApiResponse<PaymentDto>` with payment ID and intent
- **Business Logic**:
  1. Retrieve booking
  2. Validate amount matches booking total
  3. Create Payment entity with status PENDING
  4. Call payment gateway (stub) to create payment intent
  5. Store gateway response
  6. Return payment details with gateway intent token
  
- **Gateway Integration** (Stub):
  - Placeholder for Stripe, PayPal, Razorpay, etc.
  - Environment variable: `PAYMENT_GATEWAY_API_KEY`
  - Timeout: 30 seconds (configurable)

#### 2.5.3 Confirm Payment (Endpoint: POST /api/payment/confirm)
- **Request**:
  ```json
  {
    "paymentId": "long",
    "transactionId": "string",
    "status": "string"
  }
  ```
- **Response**: `ApiResponse<PaymentDto>` with confirmation
- **Business Logic**:
  1. Retrieve payment
  2. Verify transaction with gateway (if supported)
  3. If verified:
     - Update payment status to COMPLETED
     - Store transaction reference
     - Trigger booking confirmation event
  4. If verification fails:
     - Update status to FAILED
     - Return error response
  
- **Async Processing**:
  - Booking confirmation triggered asynchronously via event listener
  - Allows payment gateway to confirm via webhook

#### 2.5.4 Create Webhook (Endpoint: POST /api/payment/webhook)
- **Request** (from gateway):
  ```json
  {
    "paymentId": "long",
    "status": "completed",
    "transactionId": "string",
    "timestamp": "ISO8601"
  }
  ```
- **Response**: HTTP 200 OK
- **Business Logic**:
  1. Verify webhook signature (HMAC validation)
  2. Idempotent processing: Check if already processed (correlation ID)
  3. Update payment status
  4. Trigger booking confirmation
  5. Log webhook event for audit

#### 2.5.5 Refund Endpoint (Endpoint: POST /api/payment/refund)
- **Request**:
  ```json
  {
    "paymentId": "long",
    "amount": "double",
    "reason": "string"
  }
  ```
- **Response**: `ApiResponse<RefundDto>`
- **Business Logic**:
  1. Retrieve payment
  2. Validate refund amount <= payment amount
  3. Create Refund entity
  4. Call gateway to process refund (stub)
  5. Update payment status to REFUNDED
  6. Log refund for audit
  
- **Partial Refunds**: Supported; multiple refund records can exist per payment

---

### 2.6 Refunds Management

#### 2.6.1 Refund Entity
- **Entity**: `Refund.java`
- **Attributes**:
  - `refundId`: Unique identifier
  - `paymentId`: Reference to payment
  - `amount`: Refund amount
  - `reason`: Reason code (CUSTOMER_REQUEST, CANCELLATION, ERROR, etc.)
  - `status`: PENDING | COMPLETED | FAILED
  - `createdAt`

#### 2.6.2 Refund Policy
- **For initiate bookings (unpaid)**: No refund (delete booking)
- **For confirmed bookings within 24 hours**: Full refund allowed
- **For confirmed bookings > 24 hours**: 50% cancellation fee
- **For bookings with active tickets**: No refund (operator policy)

#### 2.6.3 Webhook for Refund Status
- **From Gateway**: Refund confirmation updates status
- **Retry Logic**: Automatic retry on failure (exponential backoff, max 3 retries)

---

### 2.7 Admin & User Management

#### 2.7.1 Manage Users (Endpoint: GET/POST/PUT /api/admin/users)
- **GET** (list users):
  - Query: `page`, `size`, `role`, `status`
  - Response: Paginated user list
  - Authorization: ADMIN only
  
- **POST** (create user):
  - Request: User registration data
  - Authorization: ADMIN only
  
- **PUT** (update user):
  - Updates: role, enabled/disabled status
  - Authorization: ADMIN only

#### 2.7.2 User Disable/Enable
- **Endpoint**: `PUT /api/admin/users/{userId}/status`
- **Request**:
  ```json
  {
    "enabled": false
  }
  ```
- **Effect**: User cannot login; existing tokens remain valid (validated on each request)
- **Audit**: Logged with admin ID and timestamp

#### 2.7.3 Audit Log Access
- **Endpoint**: `GET /api/admin/audit-logs`
- **Filters**: `action`, `userId`, `dateRange`, `entity`
- **Retention**: 1 year (configurable)
- **Authorization**: ADMIN only

---

### 2.8 Common Features

#### 2.8.1 API Response Wrapper
- **Class**: `ApiResponse<T>.java`
- **Structure**:
  ```json
  {
    "success": "boolean",
    "data": "T",
    "message": "string",
    "timestamp": "ISO8601",
    "correlationId": "string"
  }
  ```
- **Applied to**: All endpoints
- **Purpose**: Consistent response format; facilitates client parsing

#### 2.8.2 Global Exception Handling
- **Class**: `GlobalExceptionHandler.java`
- **Exceptions Handled**:
  - `EntityNotFoundException`: HTTP 404
  - `ValidationException`: HTTP 400
  - `UnauthorizedException`: HTTP 401
  - `ForbiddenException`: HTTP 403
  - `DuplicateResourceException`: HTTP 409
  - Generic `Exception`: HTTP 500 with error ID for support

#### 2.8.3 Correlation ID Logging
- **Filter**: `CorrelationIdFilter.java`
- **Generation**: UUID per request if not provided
- **Propagation**: Added to response headers, MDC, and database logs
- **Purpose**: Trace requests across distributed logs

#### 2.8.4 Idempotency
- **Filter**: `IdempotencyFilter.java`
- **Client Header**: `Idempotency-Key` (UUID)
- **Storage**: Redis cache with 24-hour TTL
- **Behavior**:
  - Same key within TTL returns cached response
  - Different key processes request normally
  - Non-idempotent methods (GET, DELETE): Bypass idempotency check

#### 2.8.5 Rate Limiting
- **Filter**: `RateLimitingFilter.java`
- **Policy**: 100 requests per hour per client IP
- **Storage**: In-memory map (sliding window)
- **Response**: HTTP 429 Too Many Requests when exceeded
- **Header**: `Retry-After` with seconds to wait
- **Exclusions**: Public endpoints (/api/auth/register, /api/auth/login)

#### 2.8.6 Audit Logging
- **Aspect**: `AuditAspect.java`
- **Annotations**: `@Auditable` on service methods
- **Logged Actions**: CREATE, UPDATE, DELETE, LOGIN, LOGOUT
- **Data Captured**:
  - User ID (from JWT)
  - Action type and timestamp
  - Entity type and ID
  - Old and new values (if available)
  - Result status (SUCCESS, FAILURE)
  
- **Storage**: Database table `audit_logs`
- **Retention Policy**: 1 year

---

## 3. Data Models & Relationships

### 3.1 Entity Relationship Diagram
```
User (1) ------- (N) Booking
  |
  +-------- (N) Passenger

Booking (1) ------- (N) BookingItem
Booking (1) ------- (1) Payment

Payment (1) ------- (N) Refund

Route (1) ------- (N) Schedule
Schedule (1) ------- (N) Seat

AuditLog (1) ------- (1) User (auditor)
```

### 3.2 Key Entities

#### User
- Represents customer, operator, or admin
- Soft delete via `enabled` flag
- Password hashed using bcrypt

#### Booking
- Aggregates seats into a single transaction
- PNR serves as customer-facing reference
- Status immutable after confirmed (audit trail)

#### Seat
- Granular inventory unit
- Status managed by booking lifecycle
- Price overridable per seat (premium seats)

#### Schedule
- Time-based snapshot of route capacity
- Base price can be overridden by seat-level pricing

---

## 4. API Security

### 4.1 Authentication
- **Method**: JWT (JSON Web Token)
- **Algorithm**: HMAC-SHA256
- **Payload Claims**:
  - `sub`: User ID
  - `username`: Username
  - `role`: User role
  - `iat`: Issued at
  - `exp`: Expiration (24 hours from issue)

### 4.2 Authorization
- **Route Protection**: 
  - `/api/auth/**`: Public
  - `/api/search/**`: Authenticated
  - `/api/booking/**`: PASSENGER+
  - `/api/admin/**`: ADMIN only
  - `/api/payment/**`: PASSENGER+ (owner validation in service)

- **Resource-Level**: Service layer validates user owns resource (booking, payment, etc.)

### 4.3 Cryptography
- **Password**: bcrypt with salt rounds=10
- **JWT Signature**: HS256 with `JWT_SECRET` (min 32 bytes)
- **Sensitive Data**: Avoid logging passwords, credit card details

### 4.4 Input Validation
- **Client-Side**: Email format, string length
- **Server-Side**: 
  - Required fields presence
  - Data type validation
  - Business rule validation (e.g., departure > arrival)
  - SQL injection prevention: Parameterized queries (JPA)
  - XSS prevention: HTMLEscape if custom rendering

---

## 5. Error Handling & Status Codes

| Code | Scenario | Response |
|------|----------|----------|
| 200  | Success  | Success=true, data populated |
| 400  | Bad Request | Validation error, field errors listed |
| 401  | Unauthorized | Missing/invalid JWT |
| 403  | Forbidden | User lacks role/permission for resource |
| 404  | Not Found | Entity does not exist |
| 409  | Conflict | Duplicate resource, status conflict |
| 429  | Too Many Requests | Rate limit exceeded |
| 500  | Server Error | Unhandled exception, error ID provided |

---

## 6. Non-Functional Requirements

### 6.1 Performance
- **Database**: H2 (in-memory for dev), PostgreSQL (production)
- **Caching**: Redis for schedule search results (1-hour TTL)
- **Pagination**: Default 20 items, max 100
- **Response Time SLA**: 95th percentile < 500ms (excluding external calls)

### 6.2 Scalability
- **Stateless API**: Horizontal scaling via load balancer
- **Database Connection Pool**: HikariCP with 10-20 connections
- **Cache**: Redis cluster for production
- **Message Queue**: Optional RabbitMQ/Kafka for async events (future)

### 6.3 Availability
- **Uptime SLA**: 99.5%
- **Backup**: Daily database backups, 30-day retention
- **Disaster Recovery**: RTO 4 hours, RPO 1 hour

### 6.4 Testability
- **Unit Tests**: ≥80% code coverage (JUnit 5, Mockito)
- **Integration Tests**: Spring Boot Test with H2 in-memory DB
- **Contract Tests**: REST Assured for API contracts
- **Performance Tests**: JMeter for load testing (optional)

### 6.5 Logging & Monitoring
- **Framework**: SLF4J + Logback
- **Levels**: DEBUG (dev), INFO (prod)
- **Correlation ID**: Propagated via MDC
- **Metrics**: Micrometer (optional Prometheus export)
- **Health Check**: `/actuator/health` endpoint (Spring Boot Actuator)

### 6.6 Documentation
- **OpenAPI/Swagger**: SpringDoc (2.1.0)
- **UI**: Swagger-UI at `/swagger-ui.html`
- **Specification**: `/v3/api-docs` (JSON)

---

## 7. Deployment & Infrastructure

### 7.1 Containerization
- **Technology**: Docker
- **Base Image**: `openjdk:21-slim`
- **Build**: Multi-stage build to minimize image size
- **Volumes**: None required (stateless)

### 7.2 Configuration Management
- **External Config**: `application.yml` (application-test.yml for tests)
- **Environment Variables**:
  - `SPRING_DATASOURCE_URL`: Database connection string
  - `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET`: Minimum 32-byte key
  - `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`
  - `SPRING_CACHE_TYPE`: Cache provider (redis/simple)
  - `PAYMENT_GATEWAY_API_KEY`: Payment provider credentials

### 7.3 Database Migrations
- **Tool**: Flyway
- **Location**: `src/main/resources/db/migration/`
- **Versioning**: V1__..., V2__..., etc.
- **Idempotency**: Undo scripts not generated; use new versions for corrections

### 7.4 Build & Release
- **Build Tool**: Maven 3.9+
- **Java Version**: 21 (LTS)
- **Build Command**: `mvn clean package -DskipTests`
- **Artifact**: Executable JAR with embedded Tomcat

---

## 8. Timeline & Roadmap

### Phase 1 (Current)
- ✅ Core entities (User, Booking, Payment)
- ✅ Authentication (JWT)
- ✅ Search & inventory
- ✅ Rate limiting & caching
- ✅ Test coverage >80%
- ✅ OpenAPI documentation

### Phase 2 (Future)
- Real payment gateway integration (Stripe/Razorpay)
- Email notifications (confirmation, receipt, refund)
- Mobile app support (native or web-based)
- Advanced reporting (revenue, occupancy, cancellations)
- Analytics pipeline (data warehouse sync)

### Phase 3 (Future)
- Machine learning for dynamic pricing
- Real-time seat availability via WebSocket
- Admin dashboard (Angular/React)
- Multi-currency support
- Internationalization (i18n)

---

## 9. Assumptions & Constraints

### 9.1 Assumptions
- Single timezone (UTC); clients handle local conversion
- Prices in base currency (e.g., USD cents)
- Seats are fixed per schedule (no dynamic allocation)
- Email notifications not required (Phase 2)
- Single payment provider (Stripe stub)

### 9.2 Constraints
- No offline functionality; API required
- JWT expiration must be validated server-side
- Refunds initiated within payment gateway (no manual transfers)
- No double-booking protection at DB level (handled in code)

---

## 10. Glossary

| Term | Definition |
|------|-----------|
| **PNR** | Passenger Name Record; unique booking confirmation number |
| **Hold** | Temporary reservation of seat(s) in HELD status with TTL |
| **Booking** | Customer's completed transaction for one or more seats |
| **Schedule** | Specific departure with date, time, and seat inventory |
| **Route** | Origin-destination pair (e.g., NYC to Boston) |
| **Seat** | Atomic inventory unit with status (AVAILABLE, HELD, BOOKED, BLOCKED) |
| **Idempotency** | Property where same request yields same result if repeated |
| **JWT** | JSON Web Token for stateless authentication |
| **TTL** | Time to live; expiration duration for cached/held resources |
| **Webhook** | HTTP callback from external service to notify of events |

---

## 11. Appendices

### 11.1 Sample API Calls

**Register User**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
```

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123"
  }'
```

**Search Routes**
```bash
curl -X GET "http://localhost:8080/api/search?origin=NYC&destination=Boston&type=BUS&page=0&size=10" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Create Booking**
```bash
curl -X POST http://localhost:8080/api/booking/initiate \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduleId": 1,
    "seatIds": [101, 102],
    "holdSessionId": "sess_abc123"
  }'
```

**Process Payment**
```bash
curl -X POST http://localhost:8080/api/payment/create-intent \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 5,
    "amount": 19999
  }'
```

---

## 12. References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Guide](https://spring.io/guides/gs/securing-web/)
- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [RESTful API Best Practices](https://restfulapi.net/)

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Author**: TicketWave Development Team
