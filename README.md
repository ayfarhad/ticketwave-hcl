# TicketWave Backend

Java 21 / Spring Boot 3 service for travel & events ticket booking.

## Features
- JWT authentication & role-based access (CUSTOMER, OPERATOR, SUPPORT, ADMIN)
- Search with caching, CQRS pattern
- Seat inventory with Redis holds
- Booking + PNR generation
- Payment & refund flows
- Rate limiting (simple in-memory limiter)
- Audit logging, correlation ID
- DTOs, global exception handling
- PostgreSQL with Flyway, Redis for caching

## Build & Run
```bash
mvn clean package
docker build -t ticketwave-backend .
```

## Tests
```bash
mvn test
```

## API Examples
```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"john","email":"john@test.com","password":"pwd"}'

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"john","password":"pwd"}'

curl http://localhost:8080/api/search?origin=A&destination=B

# after obtaining JWT
token=<<JWT>>

curl -X POST http://localhost:8080/api/bookings/initiate -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -d '[1,2]'

curl -X POST http://localhost:8080/api/payments/intent?bookingId=1&amount=200 \
    -H "Authorization: Bearer $token"

curl -X POST http://localhost:8080/api/payments/1/confirm?ref=ext123 \
    -H "Authorization: Bearer $token"
```