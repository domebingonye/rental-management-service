# Rental Management Service

A Spring Boot-based wallet and rental management platform that supports customer and merchant registration, wallet creation, virtual bank accounts, fund transfers, and asset rentals.

The application demonstrates secure authentication, transactional wallet operations, payment verification, concurrency handling, and merchant-based asset rental workflows.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot 3.4.11 | Backend framework |
| Spring Web | REST API development |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Database persistence |
| Microsoft SQL Server | Relational database |
| JWT | Stateless authentication |
| BCrypt | Password hashing |
| Hibernate | ORM |
| Swagger / OpenAPI | API documentation |
| Maven | Build and dependency management |

## Core Features

### 1. User Management and Authentication

The application supports three user roles:

- **CUSTOMER:** Can fund wallets, transfer money, and rent assets.
- **MERCHANT:** Can manage assets and receive rental payments.
- **STAFF:** Can manage users and approve merchant registrations.

Features include:

- Customer registration.
- Merchant registration.
- Automatic wallet creation during registration.
- Automatic virtual bank account generation.
- JWT-based authentication.
- Role-based access control.
- Merchant approval workflow.
- User suspension and reactivation.
- Secure password hashing using BCrypt.

Customer registration creates an active customer account, wallet, and virtual bank account in one database transaction.

Merchant registration creates the user, merchant profile, wallet, and virtual bank account in one transaction. Newly registered merchants remain in `PENDING_APPROVAL` status until approved by staff.

### 2. Wallet Management

Every registered customer and merchant receives a wallet.

Wallet features include:

- Wallet balance enquiry.
- Wallet funding.
- Wallet-to-wallet transfers.
- Transaction history.
- Transaction references.
- Balance validation.
- Protection against negative balances.

All monetary values are represented using `BigDecimal` to avoid floating-point precision issues.

Every wallet balance update is accompanied by a corresponding wallet transaction record containing:

- Transaction type.
- Transaction amount.
- Previous balance.
- New balance.
- Transaction reference.

### 3. Bank-to-Wallet Funding

The application includes a mock bank integration to simulate external bank transfers and payment provider interactions.

The funding workflow supports:

1. Initiating a bank transfer.
2. Returning a pending transfer response.
3. Simulating successful or failed settlement.
4. Sending an HMAC-SHA512 signed webhook.
5. Validating the webhook signature.
6. Re-querying the payment provider to verify the transaction.
7. Crediting the wallet only after successful verification.

Webhook events are deduplicated using the provider session ID to prevent duplicate wallet credits.

The mock bank allows the funding workflow to be tested without connecting to a real banking provider.

### 4. Wallet-to-Wallet Transfers

Customers can transfer funds directly to approved merchants.

The transfer process:

1. Customer supplies the recipient wallet number.
2. The system validates the recipient.
3. Customer initiates the transfer.
4. The system checks wallet balances.
5. The sender's wallet is debited.
6. The recipient's wallet is credited.
7. The transaction is recorded.

The debit, credit, and transfer record are processed within a single database transaction.

Wallets are locked in ascending ID order to reduce deadlock risks during concurrent transfers.

Transaction references support idempotency, preventing duplicate processing of the same request.

### 5. Asset Management

Approved merchants can create and manage rental assets.

Each asset contains information such as:

- Asset name.
- Asset category.
- Description.
- Daily rental rate.
- Available quantity.
- Asset location.
- Merchant ownership.

Example asset:

```json
{
  "name": "Honda EU22i Generator",
  "category": "EQUIPMENT",
  "dailyRate": 15000,
  "quantity": 3,
  "location": "Lekki Phase 1"
}
```

### 6. Rental Management

Customers can rent available assets from approved merchants.

The rental workflow includes:

1. Customer selects an asset.
2. Customer provides the rental start and end dates.
3. The system validates asset availability.
4. Rental duration is calculated.
5. The rental amount is calculated using the merchant's configured rate.
6. Customer wallet balance is validated.
7. Customer wallet is debited.
8. Merchant wallet is credited.
9. Asset quantity is reduced.
10. Rental information is persisted.

The rental operation is executed within a database transaction to ensure that payment, inventory updates, and rental records remain consistent.

## Application Architecture

The application follows a layered architecture:

```text
Controller
    |
    v
Service
    |
    v
DaoService
    |
    v
Repository
    |
    v
Database
```

### Architectural Responsibilities

| Layer | Responsibility |
|---|---|
| Controller | Exposes REST endpoints and handles HTTP requests |
| Service | Contains business logic and coordinates operations |
| DaoService | Handles data access operations |
| Repository | Provides database persistence through Spring Data JPA |
| Model | Defines database entities |
| DTO | Defines request and response objects |
| Security | Handles authentication and authorization |
| Exception | Centralizes application exception handling |

Services interact with the database through the DaoService layer rather than accessing repositories directly.

## Project Structure

```text
rentalservice/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ...
│   │   │           ├── config/
│   │   │           ├── constant/
│   │   │           ├── controller/
│   │   │           ├── dto/
│   │   │           ├── enums/
│   │   │           ├── exception/
│   │   │           ├── filter/
│   │   │           ├── model/
│   │   │           ├── repository/
│   │   │           ├── service/
│   │   │           └── utils/
│   │   └── resources/
│   └── test/
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Prerequisites

Ensure the following are installed:

- Java 21 or later.
- Microsoft SQL Server.
- Maven (optional, since the project includes Maven Wrapper).
- Git.

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/domebingonye/rental-management-service.git
```

### 2. Navigate to the Project

```bash
cd rental-management-service/rentalservice
```

### 3. Configure the Database

Create a SQL Server database:

```sql
CREATE DATABASE RENTALDB;
```

Configure the database connection in your application configuration.

Use environment variables or an external configuration file for database credentials and application secrets. Do not commit production credentials or JWT signing keys to source control.

### 4. Configure Environment Variables

Configure the required database connection details and JWT signing key for your environment.

The application also supports the following staff bootstrap variables:

```properties
STAFF_BOOTSTRAP_USERNAME=admin
STAFF_BOOTSTRAP_PASSWORD=your-secure-password
```

A default staff account is provisioned during the first application startup if one does not already exist. Override the bootstrap credentials before deploying the application.

### 5. Run the Application

Using Maven Wrapper on Linux or macOS:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

### 6. Access the Application

Base URL:

```text
http://localhost:6060/rental-service
```

Swagger UI:

```text
http://localhost:6060/rental-service/swagger-ui/index.html
```

## API Documentation

All application endpoints are exposed under:

```text
/rental-service
```

### Authentication Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/authentication/signup/customer` | Register a customer |
| POST | `/api/v1/authentication/signup/merchant` | Register a merchant |
| POST | `/api/v1/authentication/login` | Authenticate a user |
| POST | `/api/v1/authentication/staff` | Create a staff account |

### Staff Management

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/staff/merchants/pending` | List pending merchant applications |
| PATCH | `/api/v1/staff/merchants/{id}/approve` | Approve a merchant |
| PATCH | `/api/v1/staff/users/{id}/suspend` | Suspend a user |
| PATCH | `/api/v1/staff/users/{id}/reactivate` | Reactivate a user |

### Wallet Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/wallet` | Get wallet details |
| GET | `/api/v1/wallet/transactions` | Get wallet transaction history |
| POST | `/api/v1/wallet/fund` | Direct development funding shortcut |
| GET | `/api/v1/wallet/transfers/recipient` | Validate transfer recipient |
| POST | `/api/v1/wallet/transfers` | Transfer funds to another wallet |

### Bank Transfer Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/mock-bank/api/v1/transfers` | Simulate a bank transfer |
| GET | `/api/v1/inbound-transfers` | View inbound transfer records |
| POST | `/api/v1/webhooks/bank` | Receive bank transfer webhook events |

### Asset Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/assets` | Create a rental asset |
| GET | `/api/v1/assets` | List available assets |
| GET | `/api/v1/assets/{id}` | Get asset details |

### Rental Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/rentals` | Rent an asset |
| GET | `/api/v1/rentals` | View rental records |

Refer to Swagger UI for the complete endpoint definitions, request parameters, and response schemas.

## Authentication and Authorization

The application uses Spring Security and JWT for authentication.

After a successful login, the application returns a JWT access token.

Include the token in subsequent authenticated requests:

```http
Authorization: Bearer YOUR_ACCESS_TOKEN
```

Access is controlled using role-based authorization.

Supported roles:

```text
CUSTOMER
MERCHANT
STAFF
```

Merchant users must be approved before they can access merchant functionality.

## Transaction Integrity and Concurrency Handling

Financial operations require consistency and protection against concurrent updates.

The application implements the following measures:

- `@Transactional` for atomic database operations.
- `BigDecimal` for monetary calculations.
- Pessimistic wallet locking using SQL Server `UPDLOCK`.
- Consistent wallet locking order to reduce deadlocks.
- Idempotency using transaction references.
- Balance validation before debiting.
- Webhook signature verification.
- Duplicate webhook event detection.

These mechanisms help prevent duplicate processing, inconsistent wallet balances, and concurrent balance update issues.

## Testing the Application

A typical end-to-end test flow is:

1. Register a customer.
2. Register a merchant.
3. Log in as staff.
4. Approve the merchant.
5. Log in as the customer.
6. Fund the customer's wallet using the mock bank transfer workflow.
7. Verify that the wallet has been credited.
8. Log in as the merchant.
9. Create a rental asset.
10. Log in as the customer.
11. Rent the merchant's asset.
12. Verify the customer's debit, merchant's credit, and rental record.

Swagger UI can be used to execute and inspect the API requests.

## Current Limitations

The current implementation focuses on registration, wallet operations, bank funding simulation, and asset rental.

The following capabilities are not yet implemented:

- Rental cancellation and refund processing.
- Asset return and inventory restoration.
- Merchant wallet withdrawals.
- Persistent mock bank settlement scheduling across application restarts.
- JWT refresh tokens and server-side token revocation.
- Comprehensive automated integration and concurrency tests.
- Integration with a real banking or payment provider.

The direct wallet funding endpoint is a development shortcut. In a production implementation, wallet funding should only happen after verified payment-provider confirmation.

## Future Improvements

Potential improvements include:

- Double-entry accounting ledger.
- Automated reconciliation.
- Merchant settlement and withdrawal workflows.
- Rental cancellation and refund management.
- Asset return and maintenance workflows.
- Payment provider integration.
- Automated unit, integration, and concurrency tests.
- Docker-based deployment.
- Monitoring, audit logging, and observability.

## Security Considerations

- Passwords are hashed using BCrypt.
- JWT signing keys should be securely configured outside source control.
- Database credentials should be stored in environment variables or a secrets manager.
- Webhook signatures are validated before processing.
- Protected endpoints require authentication and appropriate authorization.
- Financial operations are executed transactionally.

## Repository

GitHub: https://github.com/domebingonye/rental-management-service

## Author

**Dominic Ebingonye**

Backend Software Engineer | Java | Spring Boot | Fintech Systems

