# Swagger API Test Scenarios - TicketWave Backend

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Purpose**: Step-by-step guide for testing TicketWave APIs using Swagger UI  
**Base URL**: `http://localhost:8080`  
**Swagger UI**: `http://localhost:8080/swagger-ui.html`

---

## Table of Contents

1. [Pre-requisites & Setup](#1-pre-requisites--setup)
2. [Test Data Reference](#2-test-data-reference)
3. [Authentication & Authorization Tests](#3-authentication--authorization-tests)
4. [Search & Inventory Tests](#4-search--inventory-tests)
5. [Booking Workflow Tests](#5-booking-workflow-tests)
6. [Payment Processing Tests](#6-payment-processing-tests)
7. [Admin Operations Tests](#7-admin-operations-tests)
8. [Error Scenario Tests](#8-error-scenario-tests)
9. [End-to-End Journey Tests](#9-end-to-end-journey-tests)

---

## 1. Pre-requisites & Setup

### 1.1 Starting the Application

```bash
# Terminal 1: Start the application
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
mvn spring-boot:run

# Expected output:
# Started TicketWaveApplication in X.XXX seconds
# Tomcat started on port(s): 8080
```

### 1.2 Accessing Swagger UI

1. Open browser: `http://localhost:8080/swagger-ui.html`
2. You should see all API endpoints grouped by tags:
   - **Authentication** (AuthController)
   - **Booking** (BookingController)
   - **Search** (SearchController)
   - **Payment** (PaymentController)
   - **Admin** (AdminController)
   - **Inventory** (InventoryController)

### 1.3 Test Data Seeding (Optional)

If you want to seed initial data before running tests:

```sql
-- Execute in PostgreSQL/H2
INSERT INTO routes (id, origin, destination, transport_type, created_at, updated_at) VALUES
(1, 'New York', 'Los Angeles', 'FLIGHT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Boston', 'Miami', 'FLIGHT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'San Francisco', 'Seattle', 'BUS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO schedules (id, route_id, departure_time, arrival_time, base_price, created_at, updated_at) VALUES
(1, 1, '2026-03-15 08:00:00', '2026-03-15 14:00:00', 299.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, '2026-03-20 10:00:00', '2026-03-20 16:00:00', 199.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 3, '2026-03-18 12:00:00', '2026-03-18 18:00:00', 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO seats (id, schedule_id, seat_number, class, status, created_at, updated_at) VALUES
(1, 1, '1A', 'BUSINESS', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, '1B', 'BUSINESS', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, '2A', 'ECONOMY', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, '2B', 'ECONOMY', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, '1A', 'BUSINESS', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 3, '1A', 'STANDARD', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

---

## 2. Test Data Reference

### 2.1 User Accounts

| Role | Username | Password | Email |
|------|----------|----------|-------|
| **Passenger** | passenger1 | Pass@123 | passenger1@example.com |
| **Passenger** | passenger2 | Pass@123 | passenger2@example.com |
| **Operator** | operator1 | Pass@123 | operator1@example.com |
| **Admin** | admin | Pass@123 | admin@example.com |

### 2.2 Routes & Schedules

| Schedule ID | Origin | Destination | Departure | Price | Seats |
|-------------|--------|-------------|-----------|-------|-------|
| **1** | New York | Los Angeles | 2026-03-15 08:00 | $299.99 | 1A, 1B, 2A, 2B |
| **2** | Boston | Miami | 2026-03-20 10:00 | $199.99 | 1A, 1B, 1C |
| **3** | San Francisco | Seattle | 2026-03-18 12:00 | $99.99 | 1A, 2A, 3A |

### 2.3 Seat Details

| Seat ID | Schedule | Number | Class | Status |
|---------|----------|--------|-------|--------|
| **1** | 1 | 1A | BUSINESS | AVAILABLE |
| **2** | 1 | 1B | BUSINESS | AVAILABLE |
| **3** | 1 | 2A | ECONOMY | AVAILABLE |
| **4** | 1 | 2B | ECONOMY | AVAILABLE |
| **5** | 2 | 1A | BUSINESS | AVAILABLE |
| **6** | 3 | 1A | STANDARD | AVAILABLE |

---

## 3. Authentication & Authorization Tests

### Test 3.1: User Registration

**Endpoint**: `POST /api/auth/register`

**Step 1**: Click on **POST /api/auth/register** in Swagger

**Request Body**:
```json
{
  "username": "passenger1",
  "email": "passenger1@example.com",
  "password": "Pass@123",
  "role": "PASSENGER"
}
```

**Expected Response** (Code: 201 Created):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "passenger1",
    "email": "passenger1@example.com",
    "role": "PASSENGER",
    "enabled": true
  },
  "message": "User registered successfully",
  "timestamp": "2026-03-03T10:30:45.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### Test 3.2: User Login

**Endpoint**: `POST /api/auth/login`

**Step 1**: Register a user first (Test 3.1) or use existing user

**Step 2**: Click on **POST /api/auth/login** in Swagger

**Request Body**:
```json
{
  "username": "passenger1",
  "password": "Pass@123"
}
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlciI6InBhc3NlbmdlcjEiLCJyb2xlIjoiUEFTU0VOR0VSIiwiaWF0IjoxNjc4MDExNDQ1LCJleHAiOjE2NzgwOTc4NDV9.abcdefghijklmnop",
    "expiresIn": 86400,
    "tokenType": "Bearer"
  },
  "message": "Login successful",
  "timestamp": "2026-03-03T10:30:45.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**⚠️ Important**: Copy the token value. You'll use it for subsequent authenticated requests.

**Step 3**: Store token for later use:
```
TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Test 3.3: Using JWT Token in Subsequent Requests

**For all authenticated endpoints**:

1. In Swagger UI, click **Authorize** button (top-right)
2. Enter: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. Click **Authorize**
4. Click **Close**

**Alternative**: Manually add header in each request:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### Test 3.4: Invalid Login (Negative Test)

**Endpoint**: `POST /api/auth/login`

**Request Body** (Wrong password):
```json
{
  "username": "passenger1",
  "password": "WrongPassword"
}
```

**Expected Response** (Code: 401 Unauthorized):
```json
{
  "success": false,
  "message": "Invalid username or password",
  "timestamp": "2026-03-03T10:31:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440002"
}
```

---

## 4. Search & Inventory Tests

### Test 4.1: Search Routes (No Authentication Required)

**Endpoint**: `GET /api/search`

**Step 1**: Click on **GET /api/search** in Swagger

**Query Parameters**:
```
origin: New York
destination: Los Angeles
departureDate: 2026-03-15
passengerCount: 2
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": [
    {
      "scheduleId": 1,
      "routeId": 1,
      "origin": "New York",
      "destination": "Los Angeles",
      "departureTime": "2026-03-15T08:00:00Z",
      "arrivalTime": "2026-03-15T14:00:00Z",
      "basePrice": 299.99,
      "availableSeats": 4,
      "transportType": "FLIGHT",
      "totalDuration": "6 hours"
    }
  ],
  "message": "Search completed successfully",
  "timestamp": "2026-03-03T10:32:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440003"
}
```

---

### Test 4.2: Search with Multiple Filters

**Endpoint**: `GET /api/search`

**Query Parameters**:
```
origin: Boston
destination: Miami
departureDate: 2026-03-20
passengerCount: 1
minPrice: 100
maxPrice: 250
seatClass: BUSINESS
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": [
    {
      "scheduleId": 2,
      "routeId": 2,
      "origin": "Boston",
      "destination": "Miami",
      "departureTime": "2026-03-20T10:00:00Z",
      "arrivalTime": "2026-03-20T16:00:00Z",
      "basePrice": 199.99,
      "availableSeats": 2,
      "transportType": "FLIGHT",
      "seatClass": "BUSINESS"
    }
  ],
  "message": "Search completed successfully",
  "timestamp": "2026-03-03T10:33:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440004"
}
```

---

### Test 4.3: Get Seats for Schedule

**Endpoint**: `GET /api/inventory/seats/{scheduleId}`

**Path Parameter**: `scheduleId = 1`

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": [
    {
      "seatId": 1,
      "scheduleId": 1,
      "seatNumber": "1A",
      "seatClass": "BUSINESS",
      "status": "AVAILABLE",
      "price": 299.99
    },
    {
      "seatId": 2,
      "scheduleId": 1,
      "seatNumber": "1B",
      "seatClass": "BUSINESS",
      "status": "AVAILABLE",
      "price": 299.99
    },
    {
      "seatId": 3,
      "scheduleId": 1,
      "seatNumber": "2A",
      "seatClass": "ECONOMY",
      "status": "AVAILABLE",
      "price": 249.99
    },
    {
      "seatId": 4,
      "scheduleId": 1,
      "seatNumber": "2B",
      "seatClass": "ECONOMY",
      "status": "AVAILABLE",
      "price": 249.99
    }
  ],
  "message": "Seats retrieved successfully",
  "timestamp": "2026-03-03T10:34:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440005"
}
```

---

### Test 4.4: No Results Found (Negative Test)

**Endpoint**: `GET /api/search`

**Query Parameters**:
```
origin: Unknown City
destination: Imaginary City
departureDate: 2026-03-25
```

**Expected Response** (Code: 200 OK - Empty):
```json
{
  "success": true,
  "data": [],
  "message": "No schedules found matching your criteria",
  "timestamp": "2026-03-03T10:35:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440006"
}
```

---

## 5. Booking Workflow Tests

### Test 5.1: Initiate Booking (Hold Seats)

**Prerequisite**: Must be logged in (have JWT token)

**Endpoint**: `POST /api/booking/initiate`

**Request Body**:
```json
{
  "scheduleId": 1,
  "seatIds": [1, 2],
  "passengerDetails": [
    {
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+1-555-0100",
      "dateOfBirth": "1990-01-15"
    },
    {
      "name": "Jane Doe",
      "email": "jane@example.com",
      "phone": "+1-555-0101",
      "dateOfBirth": "1992-03-20"
    }
  ]
}
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
Content-Type: application/json
```

**Expected Response** (Code: 201 Created):
```json
{
  "success": true,
  "data": {
    "bookingId": 1,
    "pnr": "ABC123",
    "scheduleId": 1,
    "userId": 1,
    "status": "PENDING",
    "totalAmount": 599.98,
    "holdExpiresAt": "2026-03-03T10:45:00Z",
    "passengers": [
      {
        "passengerId": 1,
        "name": "John Doe",
        "seatNumber": "1A",
        "status": "HELD"
      },
      {
        "passengerId": 2,
        "name": "Jane Doe",
        "seatNumber": "1B",
        "status": "HELD"
      }
    ],
    "createdAt": "2026-03-03T10:40:00Z"
  },
  "message": "Booking initiated successfully. Seats held for 5 minutes.",
  "timestamp": "2026-03-03T10:40:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440007"
}
```

**Store these values for next test**:
```
BOOKING_ID = 1
PNR = "ABC123"
```

---

### Test 5.2: Confirm Booking (After Payment)

**Prerequisite**: Booking initiated (Test 5.1), Payment confirmed (Test 6.2)

**Endpoint**: `POST /api/booking/confirm`

**Request Body**:
```json
{
  "bookingId": 1,
  "paymentId": 1
}
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "bookingId": 1,
    "pnr": "ABC123",
    "status": "CONFIRMED",
    "totalAmount": 599.98,
    "passengers": [
      {
        "passengerId": 1,
        "name": "John Doe",
        "seatNumber": "1A",
        "status": "BOOKED"
      },
      {
        "passengerId": 2,
        "name": "Jane Doe",
        "seatNumber": "1B",
        "status": "BOOKED"
      }
    ],
    "confirmedAt": "2026-03-03T10:50:00Z"
  },
  "message": "Booking confirmed successfully. Check email for ticket.",
  "timestamp": "2026-03-03T10:50:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440008"
}
```

---

### Test 5.3: List User Bookings

**Prerequisite**: At least one booking confirmed

**Endpoint**: `GET /api/booking/list`

**Query Parameters**:
```
page: 0
size: 10
status: CONFIRMED
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "bookingId": 1,
        "pnr": "ABC123",
        "scheduleId": 1,
        "origin": "New York",
        "destination": "Los Angeles",
        "departureTime": "2026-03-15T08:00:00Z",
        "status": "CONFIRMED",
        "totalAmount": 599.98,
        "passengerCount": 2,
        "createdAt": "2026-03-03T10:40:00Z"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  },
  "message": "Bookings retrieved successfully",
  "timestamp": "2026-03-03T10:52:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440009"
}
```

---

### Test 5.4: Cancel Booking

**Endpoint**: `POST /api/booking/cancel`

**Request Body**:
```json
{
  "bookingId": 1,
  "reason": "Schedule changed"
}
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "bookingId": 1,
    "pnr": "ABC123",
    "status": "CANCELLED",
    "totalAmount": 599.98,
    "refundAmount": 599.98,
    "cancellationReason": "Schedule changed",
    "cancellationTime": "2026-03-03T11:00:00Z"
  },
  "message": "Booking cancelled successfully. Refund will be processed.",
  "timestamp": "2026-03-03T11:00:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440010"
}
```

---

### Test 5.5: Booking Not Found (Negative Test)

**Endpoint**: `POST /api/booking/confirm`

**Request Body**:
```json
{
  "bookingId": 9999,
  "paymentId": 1
}
```

**Expected Response** (Code: 404 Not Found):
```json
{
  "success": false,
  "message": "Booking with ID 9999 not found",
  "timestamp": "2026-03-03T11:01:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440011"
}
```

---

### Test 5.6: Seats Not Available (Negative Test)

**Endpoint**: `POST /api/booking/initiate`

**Request Body** (After all seats are booked):
```json
{
  "scheduleId": 1,
  "seatIds": [1, 2],
  "passengerDetails": [...]
}
```

**Expected Response** (Code: 409 Conflict):
```json
{
  "success": false,
  "message": "One or more seats are not available",
  "errors": {
    "seats": "Seat 1A is already booked"
  },
  "timestamp": "2026-03-03T11:02:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440012"
}
```

---

## 6. Payment Processing Tests

### Test 6.1: Create Payment Intent

**Prerequisite**: Booking initiated (Test 5.1)

**Endpoint**: `POST /api/payment/create-intent`

**Request Body**:
```json
{
  "bookingId": 1,
  "amount": 599.98,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "cardDetails": {
    "cardNumber": "4242424242424242",
    "expiryMonth": "12",
    "expiryYear": "2026",
    "cvv": "123",
    "cardholderName": "John Doe"
  }
}
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
Content-Type: application/json
```

**Expected Response** (Code: 201 Created):
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "bookingId": 1,
    "amount": 599.98,
    "currency": "USD",
    "status": "PENDING",
    "paymentMethod": "CREDIT_CARD",
    "clientSecret": "pi_1234567890_secret_abcdefgh",
    "externalReference": "ch_1234567890abcdef",
    "createdAt": "2026-03-03T10:42:00Z"
  },
  "message": "Payment intent created successfully",
  "timestamp": "2026-03-03T10:42:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440013"
}
```

**Store this value**:
```
PAYMENT_ID = 1
```

---

### Test 6.2: Confirm Payment

**Endpoint**: `POST /api/payment/confirm`

**Request Body**:
```json
{
  "paymentId": 1,
  "confirmationToken": "pi_1234567890_secret_abcdefgh"
}
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "bookingId": 1,
    "amount": 599.98,
    "status": "COMPLETED",
    "transactionId": "txn_1234567890",
    "confirmedAt": "2026-03-03T10:43:00Z",
    "receiptUrl": "https://receipts.example.com/receipt_12345"
  },
  "message": "Payment confirmed successfully",
  "timestamp": "2026-03-03T10:43:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440014"
}
```

---

### Test 6.3: Refund Payment

**Prerequisite**: Payment confirmed (Test 6.2) and booking cancelled (Test 5.4)

**Endpoint**: `POST /api/payment/refund`

**Request Body**:
```json
{
  "paymentId": 1,
  "amount": 599.98,
  "reason": "Booking cancelled by user"
}
```

**Headers**:
```
Authorization: Bearer <YOUR_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "refundId": 1,
    "paymentId": 1,
    "amount": 599.98,
    "status": "INITIATED",
    "reason": "Booking cancelled by user",
    "refundReference": "rfn_1234567890",
    "estimatedCompletionDate": "2026-03-05T10:43:00Z"
  },
  "message": "Refund initiated successfully. Check email for status updates.",
  "timestamp": "2026-03-03T10:44:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440015"
}
```

---

### Test 6.4: Invalid Card (Negative Test)

**Endpoint**: `POST /api/payment/create-intent`

**Request Body** (Invalid card number):
```json
{
  "bookingId": 1,
  "amount": 599.98,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "cardDetails": {
    "cardNumber": "4000000000000002",
    "expiryMonth": "12",
    "expiryYear": "2026",
    "cvv": "123"
  }
}
```

**Expected Response** (Code: 400 Bad Request):
```json
{
  "success": false,
  "message": "Invalid payment details",
  "errors": {
    "card": "Card number is invalid"
  },
  "timestamp": "2026-03-03T10:45:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440016"
}
```

---

### Test 6.5: Insufficient Funds (Negative Test)

**Endpoint**: `POST /api/payment/confirm`

**Request Body** (With insufficient balance card):
```json
{
  "paymentId": 2,
  "confirmationToken": "pi_insufficient_funds"
}
```

**Expected Response** (Code: 402 Payment Required):
```json
{
  "success": false,
  "message": "Payment failed",
  "errors": {
    "payment": "Insufficient funds"
  },
  "timestamp": "2026-03-03T10:46:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440017"
}
```

---

## 7. Admin Operations Tests

### Test 7.1: Get All Users (Admin Only)

**Prerequisite**: Login with admin account

**Endpoint**: `GET /api/admin/users`

**Query Parameters**:
```
page: 0
size: 10
role: PASSENGER
```

**Headers**:
```
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 1,
        "username": "passenger1",
        "email": "passenger1@example.com",
        "role": "PASSENGER",
        "enabled": true,
        "createdAt": "2026-03-03T10:00:00Z",
        "lastLogin": "2026-03-03T10:40:00Z"
      },
      {
        "userId": 2,
        "username": "passenger2",
        "email": "passenger2@example.com",
        "role": "PASSENGER",
        "enabled": true,
        "createdAt": "2026-03-03T10:05:00Z",
        "lastLogin": "2026-03-03T10:35:00Z"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 2,
    "totalPages": 1
  },
  "message": "Users retrieved successfully",
  "timestamp": "2026-03-03T10:55:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440018"
}
```

---

### Test 7.2: Update User Status

**Endpoint**: `PUT /api/admin/users/{userId}`

**Path Parameter**: `userId = 1`

**Request Body**:
```json
{
  "enabled": false,
  "role": "PASSENGER"
}
```

**Headers**:
```
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "passenger1",
    "email": "passenger1@example.com",
    "role": "PASSENGER",
    "enabled": false,
    "updatedAt": "2026-03-03T10:56:00Z"
  },
  "message": "User updated successfully",
  "timestamp": "2026-03-03T10:56:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440019"
}
```

---

### Test 7.3: Get Audit Logs

**Endpoint**: `GET /api/admin/audit-logs`

**Query Parameters**:
```
page: 0
size: 20
entityType: BOOKING
action: CREATE
startDate: 2026-03-03
endDate: 2026-03-04
```

**Headers**:
```
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

**Expected Response** (Code: 200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "auditId": 1,
        "userId": 1,
        "action": "CREATE",
        "entityType": "BOOKING",
        "entityId": 1,
        "oldValue": null,
        "newValue": {
          "pnr": "ABC123",
          "status": "PENDING",
          "totalAmount": 599.98
        },
        "result": "SUCCESS",
        "timestamp": "2026-03-03T10:40:00Z",
        "correlationId": "550e8400-e29b-41d4-a716-446655440007"
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "message": "Audit logs retrieved successfully",
  "timestamp": "2026-03-03T11:00:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440020"
}
```

---

### Test 7.4: Unauthorized Admin Access (Negative Test)

**Prerequisite**: Login with passenger account

**Endpoint**: `GET /api/admin/users`

**Headers**:
```
Authorization: Bearer <PASSENGER_JWT_TOKEN>
```

**Expected Response** (Code: 403 Forbidden):
```json
{
  "success": false,
  "message": "Access denied. Admin role required.",
  "timestamp": "2026-03-03T11:01:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440021"
}
```

---

## 8. Error Scenario Tests

### Test 8.1: Missing Required Field

**Endpoint**: `POST /api/auth/register`

**Request Body** (Missing email):
```json
{
  "username": "testuser",
  "password": "Pass@123",
  "role": "PASSENGER"
}
```

**Expected Response** (Code: 400 Bad Request):
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email is required"
  },
  "timestamp": "2026-03-03T11:02:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440022"
}
```

---

### Test 8.2: Invalid Email Format

**Endpoint**: `POST /api/auth/register`

**Request Body** (Invalid email):
```json
{
  "username": "testuser",
  "email": "invalid-email",
  "password": "Pass@123",
  "role": "PASSENGER"
}
```

**Expected Response** (Code: 400 Bad Request):
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format"
  },
  "timestamp": "2026-03-03T11:03:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440023"
}
```

---

### Test 8.3: Duplicate Username

**Endpoint**: `POST /api/auth/register`

**Request Body** (Existing username):
```json
{
  "username": "passenger1",
  "email": "newemail@example.com",
  "password": "Pass@123",
  "role": "PASSENGER"
}
```

**Expected Response** (Code: 409 Conflict):
```json
{
  "success": false,
  "message": "User already exists",
  "errors": {
    "username": "Username 'passenger1' is already taken"
  },
  "timestamp": "2026-03-03T11:04:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440024"
}
```

---

### Test 8.4: Missing or Invalid JWT Token

**Endpoint**: `GET /api/booking/list`

**Headers** (No Authorization header):
```
(omit Authorization header)
```

**Expected Response** (Code: 401 Unauthorized):
```json
{
  "success": false,
  "message": "Authentication required",
  "timestamp": "2026-03-03T11:05:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440025"
}
```

---

### Test 8.5: Expired or Invalid JWT Token

**Endpoint**: `GET /api/booking/list`

**Headers** (Invalid token):
```
Authorization: Bearer invalid.token.here
```

**Expected Response** (Code: 401 Unauthorized):
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "timestamp": "2026-03-03T11:06:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440026"
}
```

---

### Test 8.6: Rate Limit Exceeded

**Endpoint**: Any endpoint (after 100 requests in 1 hour)

**Expected Response** (Code: 429 Too Many Requests):
```json
{
  "success": false,
  "message": "Rate limit exceeded",
  "detail": "Maximum 100 requests per hour allowed",
  "timestamp": "2026-03-03T11:07:00.123Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440027"
}
```

**Response Headers**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1678021600
```

---

## 9. End-to-End Journey Tests

### Journey 1: Complete Passenger Booking Flow

**Scenario**: New passenger books a flight from NYC to LA

**Steps**:

#### Step 1: Register
```
POST /api/auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "role": "PASSENGER"
}
```
✓ Response: 201 Created, User ID: 1

#### Step 2: Login
```
POST /api/auth/login
{
  "username": "john_doe",
  "password": "SecurePass123"
}
```
✓ Response: 200 OK, Token: "eyJ..."

#### Step 3: Search Routes
```
GET /api/search?origin=New York&destination=Los Angeles&departureDate=2026-03-15&passengerCount=2
```
✓ Response: 200 OK, Found Schedule ID: 1

#### Step 4: View Seats
```
GET /api/inventory/seats/1
```
✓ Response: 200 OK, 4 Available Seats

#### Step 5: Initiate Booking
```
POST /api/booking/initiate
{
  "scheduleId": 1,
  "seatIds": [1, 2],
  "passengerDetails": [
    { "name": "John Doe", "email": "john@example.com", "phone": "+1-555-0100", "dateOfBirth": "1990-01-15" },
    { "name": "Jane Doe", "email": "jane@example.com", "phone": "+1-555-0101", "dateOfBirth": "1992-03-20" }
  ]
}
```
✓ Response: 201 Created, Booking ID: 1, PNR: "ABC123"

#### Step 6: Create Payment Intent
```
POST /api/payment/create-intent
{
  "bookingId": 1,
  "amount": 599.98,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "cardDetails": {
    "cardNumber": "4242424242424242",
    "expiryMonth": "12",
    "expiryYear": "2026",
    "cvv": "123",
    "cardholderName": "John Doe"
  }
}
```
✓ Response: 201 Created, Payment ID: 1

#### Step 7: Confirm Payment
```
POST /api/payment/confirm
{
  "paymentId": 1,
  "confirmationToken": "pi_..."
}
```
✓ Response: 200 OK, Status: COMPLETED

#### Step 8: Confirm Booking
```
POST /api/booking/confirm
{
  "bookingId": 1,
  "paymentId": 1
}
```
✓ Response: 200 OK, Status: CONFIRMED, PNR: "ABC123"

#### Step 9: View Bookings
```
GET /api/booking/list?page=0&size=10&status=CONFIRMED
```
✓ Response: 200 OK, 1 Confirmed Booking

**Expected Result**: ✅ Passenger successfully booked 2 seats for NYC → LA flight with PNR "ABC123"

---

### Journey 2: Cancellation & Refund Flow

**Scenario**: Passenger cancels confirmed booking and requests refund

**Steps**:

#### Step 1: Cancel Booking
```
POST /api/booking/cancel
{
  "bookingId": 1,
  "reason": "Flight schedule changed"
}
```
✓ Response: 200 OK, Refund Amount: $599.98

#### Step 2: Initiate Refund
```
POST /api/payment/refund
{
  "paymentId": 1,
  "amount": 599.98,
  "reason": "Booking cancelled by user"
}
```
✓ Response: 200 OK, Refund Status: INITIATED

#### Step 3: Check Booking Status
```
GET /api/booking/list?status=CANCELLED
```
✓ Response: 200 OK, Booking Status: CANCELLED

**Expected Result**: ✅ Refund of $599.98 initiated, expected within 3-5 business days

---

### Journey 3: Admin User Management

**Scenario**: Admin disables a fraudulent user account

**Steps**:

#### Step 1: Admin Login
```
POST /api/auth/login
{
  "username": "admin",
  "password": "AdminPass123"
}
```
✓ Response: 200 OK, Admin Token: "eyJ..."

#### Step 2: View All Users
```
GET /api/admin/users?page=0&size=10
```
✓ Response: 200 OK, 5 Users Listed

#### Step 3: Disable User
```
PUT /api/admin/users/1
{
  "enabled": false,
  "role": "PASSENGER"
}
```
✓ Response: 200 OK, User Disabled

#### Step 4: Check Audit Log
```
GET /api/admin/audit-logs?actiontype=UPDATE&entityType=USER&entityId=1
```
✓ Response: 200 OK, Audit Entry: "User (ID: 1) disabled by admin"

**Expected Result**: ✅ User account disabled, audit trail created

---

## Appendix: Quick Reference

### Common Test Data Values

**Login Credentials** (Pre-seeded):
```
Username: passenger1    | Password: Pass@123
Username: admin         | Password: Pass@123
```

**Schedule IDs**:
```
1: NYC → LA (Mar 15, $299.99)
2: Boston → Miami (Mar 20, $199.99)
3: SF → Seattle (Mar 18, $99.99)
```

**Seat IDs for Schedule 1**:
```
1: 1A (BUSINESS) | 2: 1B (BUSINESS) | 3: 2A (ECONOMY) | 4: 2B (ECONOMY)
```

### Token Management in Swagger

1. **After Login**, copy token from response
2. Click **Authorize** (top-right corner)
3. Paste: `Bearer eyJhbGciOiJIUzI1NiIs...`
4. Click **Authorize**
5. All subsequent requests will include token automatically

### Useful HTTP Status Codes Reference

| Code | Meaning | When to Check |
|------|---------|----------|
| 200 | OK | Successful GET/PUT |
| 201 | Created | POST success |
| 400 | Bad Request | Validation errors, missing fields |
| 401 | Unauthorized | Invalid/missing JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Status conflict, duplicate data |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Server Error | Unhandled exception |

---

**Document Version**: 1.0  
**Created**: 2026-03-03  
**Testing Environment**: Local (localhost:8080)  
**Swagger UI**: http://localhost:8080/swagger-ui.html
