#  Seat Reservation API — Carrefour Kata

##  Overview

REST API for event seat reservation built with Spring Boot.
Handles seat availability, temporary holds during checkout,
and automatic expiration of unpaid reservations.

##  Architecture

Layered architecture (Controller → Service → Repository → Domain)

- **Controller** : HTTP layer, request/response handling
- **Service** : Business logic (seat availability, expiration rules)
- **Repository** : Data access via Spring Data JPA
- **Domain** : Core entities (Event, Seat, Reservation)

##  Key Design Decisions

### Seat Status vs Reservation Status
A Seat has its own lifecycle (AVAILABLE → HELD → BOOKED),
independent from the Reservation status (PENDING → CONFIRMED → EXPIRED).
This allows clean state management at both levels.

### Concurrency
Used JPA Optimistic Locking (@Version on Seat entity) to handle
concurrent reservation attempts. On conflict, the API returns 409 Conflict.
Chosen over Pessimistic Locking as simultaneous conflicts are rare
in a ticketing context.

### Expiration Mechanism
A @Scheduled job runs every 60 seconds to expire PENDING reservations
older than 15 minutes. Seats are automatically released back to AVAILABLE.
In a production context, this could be replaced by an event-driven
approach (e.g. Kafka delayed events) for better scalability.

### Hold Duration
15 minutes — consistent with industry standards (SNCF, Ticketmaster).

##  Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+

### Run the application

```bash
mvn spring-boot:run


Run the tests

mvn test


The application starts on http://localhost:8080
 API Endpoints

|Method|Endpoint |Description |
|------|------------------------------------|---------------------------|
|POST |`/api/events` |Create an event |
|GET |`/api/events/{id}` |Get event details and seats|
|POST |`/api/events/{eventId}/reservations`|Hold a seat (PENDING) |
|POST |`/api/reservations/{id}/confirm` |Confirm payment |
|DELETE|`/api/reservations/{id}` |Cancel a reservation |

 Example Usage
Create an event

POST /api/events
{
"name": "Concert Paris",
"totalCapacity": 100
}


Hold a seat

POST /api/events/1/reservations
{
"seatId": 5,
"customerEmail": "john@example.com"
}


Confirm payment

POST /api/reservations/1/confirm


 Database
Uses H2 in-memory database for simplicity.
Console available at http://localhost:8080/h2-console
 Configuration (application.properties)

spring.datasource.url=jdbc:h2:mem:kata
spring.datasource.driverClassName=org.h2.Driver
spring.h2.console.enabled=true
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=create-drop



---