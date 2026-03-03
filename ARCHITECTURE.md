# Project Architecture Documentation - TicketWave Backend

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Architecture Level**: Enterprise-Grade Microservices-Ready  
**Technology Stack**: Spring Boot 3.1.0, Java 21 LTS, PostgreSQL, Redis, Flyway

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Layered Architecture](#2-layered-architecture)
3. [Module Decomposition](#3-module-decomposition)
4. [Package Structure](#4-package-structure)
5. [Component Interaction](#5-component-interaction)
6. [Data Architecture](#6-data-architecture)
7. [API Architecture](#7-api-architecture)
8. [Security Architecture](#8-security-architecture)
9. [Caching Architecture](#9-caching-architecture)
10. [Deployment Architecture](#10-deployment-architecture)
11. [Design Patterns](#11-design-patterns)
12. [Technology Stack](#12-technology-stack)
13. [Scalability & Performance](#13-scalability--performance)

---

## 1. Architecture Overview

### 1.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                            │
│  (Web Browser / Mobile App / Third-Party Integration)           │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                    HTTP/HTTPS (REST API)
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                      API GATEWAY & FILTERS                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Rate Limiting │ Correlation ID │ Idempotency │ CORS    │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  ┌──────────────────┬──────────────────┬──────────────────┐    │
│  │  Auth Controller │ Booking Controller│ Search Controller│    │
│  │  Payment Ctrl    │ Admin Controller  │ Inventory Ctrl   │    │
│  └──────────────────┴──────────────────┴──────────────────┘    │
└──────────────────────────────┬──────────────────────────────────┘
                               │
│                              │
┌──────────────────────────────▼──────────────────────────────────┐
│                    BUSINESS LOGIC LAYER                         │
│  ┌──────────────┬──────────────┬──────────────┬────────────┐   │
│  │ Auth Service │ Search Svc   │ Booking Svc  │ Payment Svc│   │
│  │ Inventory ..│ User Service │ Refund Svc   │ Admin Svc  │   │
│  └──────────────┴──────────────┴──────────────┴────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Mappers (DTO ↔ Entity)  │  Validators  │  Utils        │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────┐
│                   DATA ACCESS LAYER (JPA)                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  UserRepository  │  BookingRepository  │  SeatRepository │  │
│  │  PaymentRepo     │  ScheduleRepository │  RouteRepository  │  │
│  │  RefundRepository│  AuditLogRepository │  ...              │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   PostgreSQL     │  │     Redis        │  │  External APIs   │
│   Database       │  │   Cache Layer    │  │  Payment Gateway │
│   (Persistent)   │  │   (Session Mgmt) │  │  Email Service   │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

### 1.2 System Architecture Principles

| Principle | Implementation |
|-----------|-----------------|
| **Separation of Concerns** | Layered architecture; each layer has specific responsibility |
| **Single Responsibility** | Each service class handles one domain entity/process |
| **Dependency Injection** | Spring IoC container manages dependencies |
| **Stateless API** | No session state stored on server; JWT for authentication |
| **Scalability** | Horizontal scaling via load balancer; PostgreSQL connection pooling |
| **High Availability** | Redundancy via container orchestration (K8s future); graceful degradation |
| **Testability** | Interfaces, mocks, dependency injection support unit testing |
| **Security** | Defence-in-depth: authentication, authorization, input validation, encryption |

---

## 2. Layered Architecture

### 2.1 Presentation Layer (Controllers)

**Responsibility**: Handle HTTP requests/responses, map HTTP → domain objects

**Key Classes**:
```
com.ticketwave.auth.controller.AuthController
  ├─ POST /api/auth/register
  ├─ POST /api/auth/login
  └─ POST /api/auth/refresh

com.ticketwave.booking.controller.BookingController
  ├─ POST /api/booking/initiate
  ├─ POST /api/booking/confirm
  ├─ POST /api/booking/cancel
  └─ GET /api/booking/list

com.ticketwave.search.controller.SearchController
  └─ GET /api/search?origin=X&destination=Y

com.ticketwave.payment.controller.PaymentController
  ├─ POST /api/payment/create-intent
  ├─ POST /api/payment/confirm
  ├─ POST /api/payment/refund
  └─ POST /api/payment/webhook

com.ticketwave.inventory.controller.InventoryController
  ├─ POST /api/inventory/hold
  └─ POST /api/inventory/release

com.ticketwave.admin.controller.AdminController
  ├─ GET /api/admin/users
  ├─ POST /api/admin/users
  ├─ PUT /api/admin/users/{id}
  └─ GET /api/admin/audit-logs
```

**Design Pattern**: **Controller (MVC)**
- One controller per resource/entity
- Delegates business logic to service layer
- Handles request validation, authentication checks

**Dependencies**: Auth service, Business services, Mappers

### 2.2 Service Layer (Business Logic)

**Responsibility**: Encapsulate business rules, orchestrate transactions, manage workflow

**Architecture**:
```java
public interface BookingService {
    Booking createBooking(Long userId, Long scheduleId, List<Long> seatIds);
    Booking confirmBooking(Long bookingId, Long paymentId);
    Booking cancelBooking(Long bookingId, String reason);
    Page<BookingDto> listUserBookings(Long userId, Pageable pageable);
}

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepo;
    private final SeatRepository seatRepo;
    private final PnrGenerator pnrGenerator;
    private final BookingMapper bookingMapper;
    
    @Transactional
    public Booking createBooking(...) {
        // Validation
        // Entity creation
        // Repository save
        // Event publishing
    }
}
```

**Service Hierarchy**:
```
Service Layer (Business Logic)
├─ AuthService/AuthServiceImpl
│  └─ User registration, login, JWT generation
├─ BookingService/BookingServiceImpl
│  └─ Create, confirm, cancel bookings
├─ PaymentService/PaymentServiceImpl
│  └─ Payment intents, confirmations, refunds
├─ SearchService/SearchServiceImpl
│  └─ Route/schedule search with caching
├─ InventoryService/InventoryServiceImpl
│  └─ Seat holds, releases, status management
├─ UserService/UserServiceImpl
│  └─ User profile management
└─ AdminService/AdminServiceImpl
   └─ User management, audit logs
```

**Design Patterns**:
- **Service Interface**: Define contracts; facilitate testing with mocks
- **Dependency Injection**: Spring manages lifecycle
- **Transactional**: `@Transactional` ensures ACID properties
- **Event-Driven**: Publish events (payment confirmation, booking confirmed) for async processing

**Dependencies**: Repositories, Mappers, External services (payment gateway, email)

### 2.3 Data Access Layer (Repositories)

**Responsibility**: Abstract database operations; provide ORM-backed data access

**Technology**: Spring Data JPA

**Repository Pattern**:
```java
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Page<Booking> findByUserIdAndStatus(Long userId, String status, Pageable pageable);
    Optional<Booking> findByPnr(String pnr);
}

// Query methods auto-generated by Spring Data
// Custom queries via @Query annotation
```

**Repository Classes**:
```
com.ticketwave.booking.BookingRepository
com.ticketwave.booking.BookingItemRepository
com.ticketwave.user.UserRepository
com.ticketwave.user.PassengerRepository
com.ticketwave.payment.PaymentRepository
com.ticketwave.payment.RefundRepository
com.ticketwave.inventory.SeatRepository
com.ticketwave.inventory.ScheduleRepository
com.ticketwave.inventory.RouteRepository
com.ticketwave.common.audit.AuditLogRepository
```

**Design Patterns**:
- **Repository**: Encapsulate data access logic
- **Specification**: Dynamic query building (if needed)
- **Pagination**: `Page<T>` for large result sets

---

## 3. Module Decomposition

### 3.1 Domain Modules

**Module Structure**:
```
src/main/java/com/ticketwave/
├── auth/                          # Authentication & Authorization
│   ├── JwtUtils.java             # JWT token generation/validation
│   ├── JwtFilter.java            # Security filter for JWT
│   ├── SecurityConfig.java       # Spring Security configuration
│   ├── controller/AuthController.java
│   ├── dto/LoginRequest.java, RegisterRequest.java
│   ├── service/AuthService.java, AuthServiceImpl.java
│   └── ...
│
├── booking/                       # Booking Management
│   ├── Booking.java              # JPA entity
│   ├── BookingItem.java
│   ├── BookingRepository.java
│   ├── BookingItemRepository.java
│   ├── controller/BookingController.java
│   ├── dto/BookingDto.java
│   ├── service/
│   │   ├── BookingService.java (interface)
│   │   ├── BookingServiceImpl.java
│   │   └── BookingMapper.java (MapStruct)
│   └── ...
│
├── payment/                       # Payment Processing
│   ├── Payment.java              # JPA entity
│   ├── Refund.java
│   ├── PaymentRepository.java
│   ├── RefundRepository.java
│   ├── controller/PaymentController.java
│   ├── dto/PaymentDto.java, RefundDto.java
│   ├── service/
│   │   ├── PaymentService.java
│   │   ├── PaymentServiceImpl.java
│   │   └── PaymentMapper.java
│   └── ...
│
├── inventory/                     # Inventory Management
│   ├── Route.java
│   ├── Schedule.java
│   ├── Seat.java
│   ├── RouteRepository.java, ScheduleRepository.java, SeatRepository.java
│   ├── controller/InventoryController.java
│   ├── service/
│   │   ├── InventoryService.java
│   │   └── InventoryServiceImpl.java
│   └── ...
│
├── search/                        # Search & Discovery
│   ├── controller/SearchController.java
│   ├── dto/ScheduleDto.java, SearchResultDto.java
│   ├── service/
│   │   ├── SearchService.java
│   │   └── SearchServiceImpl.java
│   └── ...
│
├── user/                          # User Management
│   ├── User.java
│   ├── Passenger.java
│   ├── UserRepository.java
│   ├── PassengerRepository.java
│   ├── controller/UserController.java
│   ├── service/UserService.java, UserServiceImpl.java
│   └── ...
│
├── admin/                         # Admin Functions
│   ├── controller/AdminController.java
│   ├── service/AdminService.java, AdminServiceImpl.java
│   └── ...
│
└── common/                        # Shared Components
    ├── ApiResponse.java          # Wrapper for all responses
    ├── BaseEntity.java           # Base for all entities (id, timestamps)
    ├── GlobalExceptionHandler.java
    ├── audit/
    │   ├── Auditable.java (annotation)
    │   ├── AuditAspect.java
    │   └── AuditLog.java
    ├── config/
    │   ├── CacheConfig.java      # Redis cache configuration
    │   ├── RedisConfig.java
    │   └── OpenApiConfig.java    # Swagger/OpenAPI setup
    ├── filter/
    │   ├── CorrelationIdFilter.java
    │   ├── IdempotencyFilter.java
    │   └── RateLimitingFilter.java
    └── util/
        └── PnrGenerator.java
```

### 3.2 Module Dependencies

```
┌─────────────────────────────────────────────────────┐
│                  Presentation Layer                 │
│  (Controllers: Auth, Booking, Payment, etc.)        │
└─────────────────────────┬───────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│              Business Logic Layer                   │
│  (Services: AuthService, BookingService, etc.)      │
│  (Mappers: BookingMapper, PaymentMapper)            │
│  (Utils: PnrGenerator)                              │
└─────────────────────────┬───────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│            Data Access Layer (JPA)                  │
│  (Repositories: BookingRepository, etc.)            │
├─────────────────────────────────────────────────────┤
│          Common Layer (Cross-Cutting)               │
│  (Filters, Aspects, Exceptions, Global Config)     │
└─────────────────────────────────────────────────────┘
```

---

## 4. Package Structure

### 4.1 Complete Directory Tree

```
src/
├── main/
│   ├── java/com/ticketwave/
│   │   ├── TicketWaveApplication.java         # Spring Boot entry
│   │   ├── auth/
│   │   │   ├── JwtFilter.java
│   │   │   ├── JwtUtils.java
│   │   │   ├── SecurityConfig.java
│   │   │   ├── controller/AuthController.java
│   │   │   ├── dto/LoginRequest.java, RegisterRequest.java
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       └── AuthServiceImpl.java
│   │   │
│   │   ├── booking/
│   │   │   ├── Booking.java         # @Entity
│   │   │   ├── BookingItem.java
│   │   │   ├── BookingRepository.java
│   │   │   ├── BookingItemRepository.java
│   │   │   ├── controller/BookingController.java
│   │   │   ├── dto/BookingDto.java
│   │   │   └── service/
│   │   │       ├── BookingService.java
│   │   │       ├── BookingServiceImpl.java
│   │   │       └── BookingMapper.java      # MapStruct
│   │   │
│   │   ├── payment/
│   │   │   ├── Payment.java         # @Entity
│   │   │   ├── Refund.java
│   │   │   ├── PaymentRepository.java
│   │   │   ├── RefundRepository.java
│   │   │   ├── controller/PaymentController.java
│   │   │   ├── dto/PaymentDto.java, RefundDto.java
│   │   │   └── service/
│   │   │       ├── PaymentService.java
│   │   │       ├── PaymentServiceImpl.java
│   │   │       └── PaymentMapper.java
│   │   │
│   │   ├── inventory/
│   │   │   ├── Route.java           # @Entity
│   │   │   ├── Schedule.java
│   │   │   ├── Seat.java
│   │   │   ├── RouteRepository.java
│   │   │   ├── ScheduleRepository.java
│   │   │   ├── SeatRepository.java
│   │   │   ├── controller/InventoryController.java
│   │   │   └── service/
│   │   │       ├── InventoryService.java
│   │   │       └── InventoryServiceImpl.java
│   │   │
│   │   ├── search/
│   │   │   ├── controller/SearchController.java
│   │   │   ├── dto/ScheduleDto.java, SearchResultDto.java
│   │   │   └── service/
│   │   │       ├── SearchService.java
│   │   │       └── SearchServiceImpl.java
│   │   │
│   │   ├── user/
│   │   │   ├── User.java            # @Entity
│   │   │   ├── Passenger.java
│   │   │   ├── UserRepository.java
│   │   │   ├── PassengerRepository.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   └── UserServiceImpl.java
│   │   │   └── controller/UserController.java
│   │   │
│   │   ├── admin/
│   │   │   ├── controller/AdminController.java
│   │   │   └── service/
│   │   │       ├── AdminService.java
│   │   │       └── AdminServiceImpl.java
│   │   │
│   │   └── common/
│   │       ├── ApiResponse.java
│   │       ├── BaseEntity.java
│   │       ├── GlobalExceptionHandler.java
│   │       ├── audit/
│   │       │   ├── Auditable.java
│   │       │   ├── AuditAspect.java
│   │       │   └── AuditLog.java   # @Entity
│   │       ├── config/
│   │       │   ├── CacheConfig.java
│   │       │   ├── RedisConfig.java
│   │       │   └── OpenApiConfig.java
│   │       ├── filter/
│   │       │   ├── CorrelationIdFilter.java
│   │       │   ├── IdempotencyFilter.java
│   │       │   └── RateLimitingFilter.java
│   │       └── util/
│   │           └── PnrGenerator.java
│   │
│   └── resources/
│       ├── application.yml          # Main config
│       ├── application-test.yml     # Test config
│       ├── db/migration/
│       │   └── V1__initial_schema.sql
│       └── META-INF/
│
└── test/
    ├── java/com/ticketwave/
    │   ├── auth/
    │   │   ├── AuthControllerTest.java
    │   │   └── AuthServiceImplTest.java
    │   ├── booking/
    │   │   ├── service/BookingServiceImplTest.java
    │   │   └── controller/BookingControllerTest.java
    │   ├── payment/
    │   │   ├── service/PaymentServiceImplTest.java
    │   │   └── controller/PaymentControllerTest.java
    │   ├── inventory/InventoryServiceTest.java
    │   ├── search/SearchServiceIntegrationTest.java
    │   ├── common/
    │   │   ├── filter/RateLimitingFilterTest.java
    │   │   ├── util/PnrGeneratorTest.java
    │   │   ├── filter/IdempotencyFilterTest.java
    │   │   └── auth/JwtUtilsTest.java
    │   └── TestDataFactory.java
    │
    └── resources/
        └── application-test.yml
```

---

## 5. Component Interaction

### 5.1 Multi-Component Interaction Diagram: Create Booking Workflow

```
┌─────────────────┐
│  HTTP Request   │
│  POST /api/     │
│  booking/       │
│  initiate       │
└────────┬────────┘
         │
         ▼
┌──────────────────────────────┐
│  CorrelationIdFilter         │
│  - Generate/extract UUID     │
│  - Add to MDC (Logs)         │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  JwtFilter                   │
│  - Extract JWT from header   │
│  - Validate signature/expiry │
│  - Set SecurityContext       │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  RateLimitingFilter          │
│  - Check request count/IP    │
│  - Return 429 if exceeded    │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  BookingController           │
│  POST /api/booking/initiate  │
│  - Validate request DTO      │
│  - Call BookingService       │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  BookingServiceImpl           │
│  createBooking()             │
│  - Verify user owns hold     │
│  - Validate seat status      │
│  - Generate PNR              │
│  - Begin transaction          │
└────────┬─────────────────────┘
         │
    ┌────┴────┬──────────┬────────────┐
    │          │          │            │
    ▼          ▼          ▼            ▼
┌───────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
│  Seat     │ │ PNR    │ │ Booking  │ │ Booking  │
│ Repository│ │Generator│ │Repository│ │ItemRepo  │
│ (validate)│ │(generate)│ │(save)    │ │(saveAll) │
└─────┬─────┘ └────┬───┘ └────┬─────┘ └────┬────┘
      │            │          │            │
      └────────────┴──────────┴────────────┘
              │
              ▼
┌──────────────────────────────┐
│ @Transactional Commit        │
│ (All changes persist)        │
├──────────────────────────────┤
│ PostgreSQL Database          │
│ (Booking, BookingItem saved) │
└──────────────────────────────┘
              │
              ▼
┌──────────────────────────────┐
│ BookingMapper (MapStruct)    │
│ Booking Entity → BookingDto  │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ ApiResponse Wrapper          │
│ { success: true,             │
│   data: BookingDto,          │
│   correlationId: "..." }     │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ HTTP 201 Created Response    │
│ (Sent to Client)             │
└──────────────────────────────┘
```

### 5.2 Service Interaction Graph

```
┌─────────────────────────────────────────────────────────┐
│                     Controllers                         │
│  AuthCtrl  BookingCtrl  PaymentCtrl  SearchCtrl  Admin │
└──────────────┬────────────────────────┬────────────────┘
               │                        │
     ┌─────────▼─────────┬──────────────▼───────────┐
     │                   │                          │
     ▼                   ▼                          ▼
┌─────────────┐   ┌──────────────┐       ┌────────────────┐
│Auth Service │   │Booking Svc   │       │Search Service  │
├─────────────┤   ├──────────────┤       ├────────────────┤
│ - Login     │   │ - Create     │  ◄────┤- Search routes │
│ - Register  │   │ - Confirm    │       │- Cache results │
│ - JWT gen   │   │ - Cancel     │       └────────────────┘
└──────┬──────┘   └──────┬───────┘
       │                 │
       │        ┌────────▼────────┐
       │        │                 │
       │        ▼                 ▼
       │    ┌──────────────┐  ┌────────────────┐
       │    │Payment Svc   │  │Inventory Svc   │
       │    ├──────────────┤  ├────────────────┤
       │    │ - Create     │  │ - Hold seats   │
       │    │ - Confirm    │  │ - Release      │
       │    │ - Refund     │  │ - Status mgmt  │
       │    └──────┬───────┘  └────────────────┘
       │           │
       │           ▼
       │    ┌──────────────┐
       │    │Refund Svc    │
       │    ├──────────────┤
       │    │ - Create     │
       │    │ - Track      │
       │    └──────────────┘
       │
       ▼
┌──────────────────┐
│User Service      │
├──────────────────┤
│ - User CRUD      │
│ - Password hash  │
└──────────────────┘
```

---

## 6. Data Architecture

### 6.1 Entity Relationship Diagram (ERD)

```
┌──────────────────┐
│      User        │
├──────────────────┤
│ id (PK)          │
│ username (UQ)    │
│ email (UQ)       │
│ password_hash    │
│ role             │
│ enabled          │
│ created_at       │
│ updated_at       │
└────────┬─────────┘
         │ (1)
         │ has many
         │ (N)
         ▼
┌──────────────────┐         ┌──────────────────┐
│    Booking       │◄────────│  BookingItem     │
├──────────────────┤ (1)  (N)├──────────────────┤
│ id (PK)          │         │ id (PK)          │
│ user_id (FK)     │         │ booking_id (FK)  │
│ pnr (UQ)         │         │ seat_id (FK)     │
│ status           │         │ price            │
│ total_amount     │         │ passenger_name   │
│ created_at       │         │ created_at       │
└────────┬─────────┘         └──────────────────┘
         │
         │ (1)
         │
         ▼ (1)
┌──────────────────────┐
│     Payment          │
├──────────────────────┤
│ id (PK)              │
│ booking_id (FK)      │
│ amount               │
│ status               │
│ external_reference   │
│ payment_method       │
│ created_at           │
└────────┬─────────────┘
         │
         │ (1) has many (N)
         ▼
┌──────────────────────┐
│      Refund          │
├──────────────────────┤
│ id (PK)              │
│ payment_id (FK)      │
│ amount               │
│ reason               │
│ status               │
│ created_at           │
└──────────────────────┘

┌──────────────────┐
│      Route       │
├──────────────────┤
│ id (PK)          │
│ origin           │
│ destination      │
│ transport_type   │
│ created_at       │
└────────┬─────────┘
         │ (1)
         │ has many (N)
         ▼
┌──────────────────┐
│    Schedule      │
├──────────────────┤
│ id (PK)          │
│ route_id (FK)    │
│ departure_time   │
│ arrival_time     │
│ base_price       │
│ created_at       │
└────────┬─────────┘
         │ (1)
         │ has many (N)
         ▼
┌──────────────────┐         ┌──────────────────┐
│      Seat        │─────►   │  SeatStatus      │
├──────────────────┤  (1)    ├──────────────────┤
│ id (PK)          │    (1)  │ AVAILABLE        │
│ schedule_id (FK) │         │ HELD             │
│ seat_number      │         │ BOOKED           │
│ class            │         │ BLOCKED          │
│ price (override) │         └──────────────────┘
│ status           │
│ created_at       │
└──────────────────┘

┌──────────────────┐
│   Passenger      │
├──────────────────┤
│ id (PK)          │
│ user_id (FK)    │
│ first_name       │
│ last_name        │
│ date_of_birth    │
│ phone            │
│ id_number        │
│ created_at       │
└──────────────────┘

┌──────────────────────┐
│    AuditLog          │
├──────────────────────┤
│ id (PK)              │
│ user_id (FK)         │
│ action               │
│ entity_type          │
│ entity_id            │
│ old_value (JSON)     │
│ new_value (JSON)     │
│ result (SUCCESS/FAIL)│
│ timestamp            │
│ correlation_id       │
└──────────────────────┘
```

### 6.2 Database Schema (DDL)

**Tables**:
1. `users` - Store user accounts
2. `passengers` - Additional passenger info
3. `routes` - Transportation routes
4. `schedules` - Route instances with times
5. `seats` - Bookable inventory units
6. `bookings` - Customer transactions
7. `booking_items` - Seats per booking
8. `payments` - Payment records
9. `refunds` - Refund records
10. `audit_logs` - Audit trail

**Key Constraints**:
- Primary Keys: `id` (auto-increment)
- Foreign Keys: Referential integrity
- Unique Constraints: `username`, `email`, `pnr` (once confirmed)
- Indexes: On frequently queried columns (user_id, created_at, status)

### 6.3 Data Relationships

| Relationship | Type | Cardinality | Notes |
|--------------|------|-------------|-------|
| User → Booking | One-to-Many | 1:N | Passenger creates multiple bookings |
| Booking → BookingItem | One-to-Many | 1:N | Booking contains multiple seats |
| BookingItem → Seat | Many-to-One | N:1 | Item references specific seat |
| Booking → Payment | One-to-One | 1:1 | One payment per booking |
| Payment → Refund | One-to-Many | 1:N | Multiple partial refunds possible |
| Route → Schedule | One-to-Many | 1:N | Route has multiple schedules |
| Schedule → Seat | One-to-Many | 1:N | Schedule has many seats |
| User → Passenger | One-to-One | 1:1 | Extended passenger info |

---

## 7. API Architecture

### 7.1 REST API Design

**Base URL**: `https://api.ticketwave.com/api`

**API Versioning**: URL-based (`/api/v1/...`) for future compatibility

**Endpoint Structure**:
```
POST   /api/auth/register              # Public
POST   /api/auth/login                 # Public
GET    /api/search                     # Authenticated
POST   /api/booking/initiate           # Authenticated
POST   /api/booking/confirm            # Authenticated
POST   /api/booking/cancel             # Authenticated
GET    /api/booking/list               # Authenticated
POST   /api/payment/create-intent      # Authenticated
POST   /api/payment/confirm            # Authenticated
POST   /api/payment/refund             # Authenticated
POST   /api/payment/webhook            # Public (signature verified)
PUT    /api/admin/users/{id}           # Admin only
GET    /api/admin/audit-logs           # Admin only
```

### 7.2 Request/Response Contract

**Standard Response Format**:
```json
{
  "success": boolean,
  "data": <T>,
  "message": string,
  "timestamp": "ISO8601",
  "correlationId": "uuid",
  "errors": {
    "field": "error message"
  }
}
```

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
X-Correlation-ID: <uuid>              # Optional
Idempotency-Key: <uuid>                # For POST (optional)
```

**Response Headers**:
```
X-Correlation-ID: <uuid>
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1234567890
```

### 7.3 HTTP Status Codes

| Code | Meaning | Use Case |
|------|---------|----------|
| 200  | OK | Successful GET, PUT |
| 201  | Created | POST success (resource created) |
| 204  | No Content | Successful deletion |
| 400  | Bad Request | Validation failure, missing field |
| 401  | Unauthorized | Missing/invalid JWT |
| 403  | Forbidden | User lacks permission |
| 404  | Not Found | Resource doesn't exist |
| 409  | Conflict | Status conflict, duplicate |
| 429  | Too Many Requests | Rate limit exceeded |
| 500  | Server Error | Unhandled exception |

---

## 8. Security Architecture

### 8.1 Authentication Flow

```
┌─────────────────────────────────────────────────────────┐
│ CLIENT: POST /api/auth/login                           │
│ Payload: { username, password }                        │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SERVER: AuthController.login()                         │
├─────────────────────────────────────────────────────────┤
│ 1. Find user by username                               │
│ 2. Compare password (BCrypt)                           │
│ 3. Check if enabled                                    │
└────────────────────────┬────────────────────────────────┘
                         │
                    ┌────┴────┐
                    │          │
              ┌─────▼──┐  ┌────▼─────┐
              │ Success│  │Fail/Error│
              └────┬───┘  └─────┬────┘
                   │            │
                   ▼            ▼
        ┌──────────────────┐  ┌──────────┐
        │ JwtUtils.generate│  │Return 401│
        │ (sign claims)    │  │Unauthorized
        └────────┬────────┘  └──────────┘
                 │
                 ▼
        ┌──────────────────┐
        │ JWT Token        │
        │ Header: { alg }  │
        │ Payload: {       │
        │   sub: userId,   │
        │   user: username,│
        │   role: admin,   │
        │   iat: 123456,   │
        │   exp: 123456+86400
        │ }                │
        │ Signature: HMAC  │
        └────────┬────────┘
                 │
                 ▼
        ┌──────────────────┐
        │ Return 200 OK    │
        │ { token: "..." } │
        └──────────────────┘
                 │
                 ▼
        ┌──────────────────┐
        │CLIENT stores JWT │
        │in localStorage   │
        └──────────────────┘
```

### 8.2 Authorization Model (RBAC)

**Roles**:
```
PASSENGER (default)
  - Search routes
  - Create/confirm bookings
  - View own bookings
  - Create payments (own bookings)
  
OPERATOR
  - PASSENGER permissions
  - Create routes/schedules
  - Manage pricing
  - View bookings (all)
  
ADMIN
  - All OPERATOR permissions
  - User management
  - Audit logs
  - System configuration
```

**Authorization Annotations**:
```java
@PreAuthorize("isAuthenticated()")                    // Any logged-in user
@PreAuthorize("hasRole('PASSENGER')")                // PASSENGER+ (includes OPERATOR, ADMIN)
@PreAuthorize("hasRole('ADMIN')")                    // ADMIN only
@PreAuthorize("@customAuthService.isBookingOwner(#bookingId, principal.userId)")  // Custom logic
```

### 8.3 Security Layers

```
┌─────────────────────────────────────────────────────────┐
│ Layer 1: Transport Security (HTTPS/TLS)               │
│ - Encrypted channel                                    │
│ - Certificate validation                              │
│ - Forward Secrecy (TLS 1.3+)                          │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 2: Authentication (JWT)                          │
│ - Token signature validation                          │
│ - Expiration check                                    │
│ - Revocation check (if needed)                        │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 3: Authorization (RBAC)                          │
│ - Role-based access control                           │
│ - Resource ownership checks                           │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 4: Input Validation                             │
│ - Field presence checks                               │
│ - Data type validation                                │
│ - Business rule validation                            │
│ - SQL injection prevention (parameterized queries)    │
└─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│ Layer 5: Sensitive Data Protection                     │
│ - Password hashing (BCrypt)                           │
│ - PII masking in logs                                 │
│ - Secure headers                                      │
└─────────────────────────────────────────────────────────┘
```

---

## 9. Caching Architecture

### 9.1 Cache Strategy

**Cache Provider**: Redis

**Cached Data**:
```
Key Pattern          | TTL   | Strategy | Update Trigger
──────────────────────────────────────────────────────
schedule:search:*    | 1h    | LRU      | Manual on update
user:*              | 30m   | LRU      | Manual on change
seat:*              | 5m    | LRU      | On hold/release
booking:*           | 10m   | LRU      | On status change
hold:sessionId:*    | 5m    | LRU      | Auto-expire
payment:intent:*    | 10m   | LRU      | On confirm
```

### 9.2 Cache Architecture Diagram

```
┌─────────────────────────────────────────────┐
│  Application Layer                          │
│  (Controllers, Services)                    │
└────────────────┬────────────────────────────┘
                 │
        ┌────────▼────────┐
        │ Check Cache?    │
        └────┬────────┬───┘
             │        │
        Hit  │        │ Miss
        ─────┼─────┐  │
             │     │  │
             ▼     │  ▼
        ┌─────┐   │ ┌──────────────────┐
        │Cache│   │ │Query Database    │
        │ Hit │   │ │(Repository)      │
        └────┬┘   │ └────────┬─────────┘
             │    │          │
             │    └──────┬───┘
             │           │
             │      ┌────▼──────────┐
             │      │Store in Cache │
             │      │(TTL set)      │
             │      └────┬──────────┘
             │           │
             └───────┬───┘
                     │
                     ▼
            ┌─────────────────┐
            │Return to Client │
            └─────────────────┘
```

### 9.3 Cache Invalidation Strategy

**Event-Driven Invalidation**:
```java
@Service
public class BookingServiceImpl {
    @Cacheable("bookings")
    public Booking getBooking(Long id) { ... }
    
    @CacheEvict(value = "bookings", key = "#id")
    @Transactional
    public Booking updateBooking(Long id, ...) { ... }
}

// Or event-based:
@EventListener
public void onBookingConfirmed(BookingConfirmedEvent event) {
    cacheManager.getCache("bookings").evict(event.getBookingId());
}
```

---

## 10. Deployment Architecture

### 10.1 Containerization (Docker)

**Dockerfile**:
```dockerfile
FROM openjdk:21-slim as builder
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

FROM openjdk:21-slim
WORKDIR /app
COPY --from=builder /app/target/ticketwave-*.jar ticketwave.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "ticketwave.jar"]
```

### 10.2 Deployment Architecture

```
┌──────────────────────────────────────────────────────┐
│           Production Environment                     │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ┌────────────────────────────────────────┐        │
│  │      Load Balancer (Nginx/HAProxy)     │        │
│  │   (SSL termination, traffic routing)   │        │
│  └──────────────────┬─────────────────────┘        │
│                     │                               │
│        ┌────────────┼────────────┐                  │
│        │            │            │                  │
│        ▼            ▼            ▼                  │
│   ┌────────┐   ┌────────┐   ┌────────┐             │
│   │ Pod 1  │   │ Pod 2  │   │ Pod 3  │             │
│   │ Java   │   │ Java   │   │ Java   │             │
│   │ 21     │   │ 21     │   │ 21     │             │
│   └────────┘   └────────┘   └────────┘             │
│        │            │            │                  │
│        └────────────┼────────────┘                  │
│                     │                               │
│        ┌────────────▼────────────┐                  │
│        │  Service Discovery      │                  │
│        │  (Kubernetes Service)   │                  │
│        └─────────────────────────┘                  │
│                                                      │
└──────────────────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
   ┌────────┐   ┌────────┐   ┌────────┐
   │Database│   │ Redis  │   │Logging │
   │(PostgreSQL)│(Cache) │   │(ELK)   │
   └────────┘   └────────┘   └────────┘
```

### 10.3 Environment Configuration

**Development**:
- H2 in-memory database
- Mocked external services
- Debug logging

**Staging**:
- PostgreSQL (test data)
- Redis (cache)
- Full logging

**Production**:
- PostgreSQL (with backups)
- Redis cluster (high availability)
- Minimal logging (INFO level)
- SSL/TLS enabled
- All external services integrated

---

## 11. Design Patterns

### 11.1 Architectural Patterns

| Pattern | Implementation | Benefit |
|---------|-----------------|---------|
| **Layered Architecture** | Presentation → Service → Data | Clear separation of concerns |
| **MVC** | Controller → Service → Repository | Standard web app structure |
| **DAO/Repository** | JpaRepository interfaces | Abstraction of data access |
| **Service Locator** | Spring IoC container | Loose coupling, easier testing |
| **Factory** | PnrGenerator, UserFactory | Encapsulate object creation |
| **Adapter** | MapStruct mappers | Bridge entities and DTOs |
| **Observer** | Event publishing (booking confirmed) | Decouple components |
| **Strategy** | Refund policy classes | Pluggable algorithms |
| **Decorator** | AspectJ for auditing | Cross-cutting concerns |
| **Singleton** | Spring @Service beans | Single instance lifecycle |

### 11.2 Creational Patterns

**Factory Pattern**:
```java
public class PnrGenerator {
    public String generate() {
        // Generate 6-char alphanumeric PNR
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
```

**Builder Pattern** (DTO construction):
```java
BookingDto dto = BookingDto.builder()
    .bookingId(booking.getId())
    .pnr(booking.getPnr())
    .status(booking.getStatus())
    .totalAmount(booking.getTotalAmount())
    .build();
```

### 11.3 Structural Patterns

**Adapter Pattern** (MapStruct):
```java
@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingDto toDto(Booking entity);
    Booking toEntity(BookingDto dto);
}
```

**Facade Pattern** (Service layer):
```java
@Service
public class BookingServiceImpl {
    // Hides complexity of hold validation, PNR generation,
    // repository interactions behind simple `createBooking()` method
    public Booking createBooking(...) { ... }
}
```

### 11.4 Behavioral Patterns

**Template Method** (Abstract base services):
```java
@Transactional
public abstract class BaseService {
    public final void process() {
        validate();
        execute();
        audit();
    }
    
    protected abstract void execute();
    protected void validate() { ... }
    protected void audit() { ... }
}
```

**Observer Pattern** (Spring Events):
```java
// Publisher
@Service
public class BookingServiceImpl {
    @Autowired private ApplicationEventPublisher eventPublisher;
    
    public Booking confirmBooking(...) {
        bookingRepo.save(booking);
        eventPublisher.publishEvent(new BookingConfirmedEvent(booking));
    }
}

// Subscriber
@Component
public class PaymentConfirmationListener {
    @EventListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        // Auto-confirm payment, send email, etc.
    }
}
```

---

## 12. Technology Stack

### 12.1 Core Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Language** | Java | 21 LTS | High-performance, type-safe |
| **Framework** | Spring Boot | 3.1.0 | REST API, IoC, Auto-config |
| **ORM** | Hibernate JPA | 6.2.2 | Object-relational mapping |
| **Build Tool** | Maven | 3.9.12 | Dependency management, build |
| **Web Server** | Tomcat | 10.1.x | Embedded servlet container |

### 12.2 Database & Persistence

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Primary DB** | PostgreSQL | 15+ | Relational data storage |
| **Dev/Test DB** | H2 | 2.1.x | In-memory for testing |
| **Migration** | Flyway | 9.16.3 | Schema versioning |
| **ORM** | Hibernate | 6.2.2 | Entity mapping, queries |
| **Query Builder** | Spring Data JPA | - | Auto-generated repositories |

### 12.3 Caching & Session

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Cache Store** | Redis | 7+ | Distributed caching |
| **Session Store** | Redis | 7+ | Distributed sessions |
| **Cache Abstraction** | Spring Cache | 3.1.0 | Unified caching API |
| **Hold Storage** | Redis | 7+ | Temporary seat holds (TTL) |

### 12.4 Security

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Security Framework** | Spring Security | 6.1.0 | Authentication, authorization |
| **JWT Library** | JJWT | 0.11.5 | JWT token generation/validation |
| **Password Hashing** | BCrypt | Built-in | Secure password storage |
| **HTTPS** | Tomcat SSL | Built-in | Encrypted transport |

### 12.5 Testing

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Unit Testing** | JUnit 5 | 5.9.x | Test framework |
| **Mocking** | Mockito | 5.2.x | Mock dependencies |
| **Integration** | Spring Boot Test | 3.1.0 | Full context testing |
| **Assertions** | AssertJ | 3.24.x | Fluent assertions |
| **Test Containers** | Testcontainers | 1.19.x | Docker-based test DBs |

### 12.6 API Documentation

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **OpenAPI Spec** | SpringDoc | 2.1.0 | Automatic API documentation |
| **UI** | Swagger UI | 4.x | Interactive API explorer |
| **Spec Gen** | Springdoc OpenAPI | 2.1.0 | JSON/YAML spec generation |

### 12.7 Code Quality & Logging

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Logging** | SLF4J + Logback | Latest | Structured logging |
| **Monitoring** | Micrometer | 1.11.x | Metrics export (Prometheus) |
| **Code Gen** | Lombok | 1.18.30 | Reduce boilerplate (@Data, @Getter) |
| **DTO/Entity Mapping** | MapStruct | 1.5.5.Final | Compile-time mapper generation |

---

## 13. Scalability & Performance

### 13.1 Horizontal Scaling

**Stateless Design**:
- No in-memory session storage
- JWT for authentication (no session affinity needed)
- Any instance can handle any request

**Load Distribution**:
```
┌─────────────┐
│  Client     │
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│  Load Balancer   │
│  (Round-robin)   │
└──┬───────────────┘
   │
   ├─────────────────────┬──────────────────┬─────────────┐
   ▼                     ▼                  ▼             ▼
┌────────────┐      ┌────────────┐     ┌────────────┐ ┌────────────┐
│ Instance 1 │      │ Instance 2 │     │ Instance 3 │ │ Instance N │
│ (Stateless)│      │ (Stateless)│     │ (Stateless)│ │ (Stateless)│
└────┬───────┘      └────┬───────┘     └────┬───────┘ └────┬───────┘
     │ (Shared cache)    │ (Shared cache)    │ (Shared cache) │
     └──────────────┬────┴──────────────┬────┴──┘────┬───────┘
                    │                  │            │
                    ▼                  ▼            ▼
               ┌──────────────────────────────────────┐
               │  Redis Cache (Cluster)               │
               │  (Shared across all instances)       │
               └──────────────────────────────────────┘
                    │
                    ▼
               ┌──────────────────────────────────────┐
               │  PostgreSQL Database (Primary)       │
               │  (Connection pooling)                │
               └──────────────────────────────────────┘
```

### 13.2 Performance Optimization

**Database**:
- Connection pooling (HikariCP): 10-20 connections
- Indexes on frequently queried columns
- Pagination for large result sets (limit 100)
- Lazy loading of relationships

**Caching**:
- Schedule search results: 1-hour TTL
- User profiles: 30-minute TTL
- Seat holds: 5-minute TTL (auto-expire)

**Code-Level**:
- Batch operations where possible
- Avoid N+1 queries (JPA fetch strategies)
- Use DTOs to limit data transfer
- Async processing for non-critical tasks

### 13.3 Monitoring & Observability

**Metrics** (Micrometer → Prometheus):
```
ticketwave_http_requests_total{method="POST", endpoint="/booking/initiate"}
ticketwave_http_request_duration_ms{endpoint="/search", quantile="0.95"}
ticketwave_sql_queries_total{operation="SELECT", table="bookings"}
ticketwave_cache_hits_total{cache="schedule_search"}
ticketwave_active_connections{pool="default"}
```

**Logging**:
```
[INFO] 2026-03-03 10:30:45.123 [correlation_id=abc-123] BookingServiceImpl - Creating booking for user 5
[DEBUG] 2026-03-03 10:30:45.456 [correlation_id=abc-123] SeatRepository - Query: SELECT s FROM Seat s WHERE schedule_id = 1
[WARN] 2026-03-03 10:30:46.789 [correlation_id=abc-123] PaymentService - Payment gateway timeout, retrying in 5s
[ERROR] 2026-03-03 10:31:00.999 [correlation_id=abc-123] BookingServiceImpl - Failed to create booking: NullPointerException
```

---

## Appendix: Architecture Decision Records (ADRs)

### ADR-001: Monolithic vs Microservices

**Decision**: Monolithic architecture (microservices-ready)

**Rationale**:
- ✅ Simpler deployment and operations (single JAR)
- ✅ Easier to debug and maintain
- ✅ Lower operational complexity
- ✅ Package structure allows future microservice extraction
- ⚠️ Scale-out may require DB optimization

---

### ADR-002: REST vs GraphQL

**Decision**: REST API

**Rationale**:
- ✅ Simplicity, wide adoption
- ✅ Browser-friendly, no query language learning curve
- ✅ Better REST tooling (caching via HTTP, standard status codes)
- ❌ Might over-fetch data for complex queries
- Future: Consider adding GraphQL endpoint in Phase 2

---

### ADR-003: JPA vs SpringData vs Raw SQL

**Decision**: JPA with Spring Data repositories

**Rationale**:
- ✅ Type-safe, compile-time checking
- ✅ Automatic pagination, sorting
- ✅ No boilerplate query methods
- ✅ Consistent transaction management
- ⚠️ Complex queries may need `@Query` annotation

---

### ADR-004: Single Database vs CQRS

**Decision**: Single database (monolithic)

**Rationale**:
- ✅ Simpler consistency model
- ✅ No eventual consistency issues
- ✅ Easier transactions
- ✅ Adequate for current scale
- Future: Consider CQRS if read volume exceeds write volume

---

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Guide](https://spring.io/guides/gs/accessing-data-jpa)
- [Spring Security Architecture](https://spring.io/guides/gs/securing-web)
- [PostgreSQL Manual](https://www.postgresql.org/docs)
- [Redis Documentation](https://redis.io/documentation)
- [JWT Introduction](https://tools.ietf.org/html/rfc7519)
- [Twelve-Factor App](https://12factor.net)
- [OWASP Application Security Architecture](https://cheatsheetseries.owasp.org)

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-03  
**Status**: Active  
**Architecture Owner**: Platform Engineering Team
