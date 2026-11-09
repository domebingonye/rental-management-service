# Rental Service — Wallet System

A Spring Boot wallet system built for the 3Line Limited Senior Backend Engineer assessment, set in a simple
rental marketplace with three kinds of users: **customers**, **merchants** and **staff**.

| Assessment requirement | Where it is covered |
|---|---|
| **1. Create a user & generate account** | Customer / merchant sign-up creates the user, a **wallet** and a **virtual bank account** in one transaction |
| **2. Fund transfer between accounts** | Bank → wallet (via the virtual account, webhook-verified), customer wallet → merchant wallet, and rental payments |

---

## Tech stack

- Java 21, Spring Boot 3.4.11 (Web, Security, Data JPA, Validation)
- Microsoft SQL Server (JPA/Hibernate creates the tables on startup)
- JWT authentication (jjwt 0.12), BCrypt password hashing
- Swagger UI (springdoc-openapi)

---

## Running the application

### 1. Database
Create a database called `RENTALDB` on SQL Server and give the application login access to it:

```sql
USE RENTALDB;
CREATE USER [Dom] FOR LOGIN [Dom];
ALTER ROLE db_owner ADD MEMBER [Dom];
```

Connection settings are in `rentalservice/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://DOMINIC:1433;databaseName=RENTALDB;encrypt=false;trustServerCertificate=true
spring.datasource.username=Dom
spring.datasource.password=123
```

### 2. Start
```bash
cd rentalservice
./mvnw spring-boot:run
```

- Base URL: `http://localhost:6060/rental-service`
- Swagger UI: `http://localhost:6060/rental-service/swagger-ui/index.html`

> Every endpoint lives under the context path **`/rental-service`**. Calling `http://localhost:6060/api/...`
> or `http://localhost:6060/mock-bank/...` without it returns 404.

### 3. First staff user
Staff cannot sign up themselves. On first startup the application creates one staff account:

| Setting | Default | Override with env var |
|---|---|---|
| `bootstrap.staff.username` | `admin` | `STAFF_BOOTSTRAP_USERNAME` |
| `bootstrap.staff.password` | `ChangeMe@123` | `STAFF_BOOTSTRAP_PASSWORD` |

It is only created when no staff user exists yet, so changing the setting later does not change an existing password.

---

## Architecture

```
Controller  →  Service  →  DaoService  →  Repository  →  SQL Server
 (HTTP, roles)  (business rules,   (persistence helpers,
                 transactions)      code generation, DTO mapping)
```

- Services never call repositories directly — only `*DaoService` classes do.
- Packages: `config`, `constant`, `controller`, `dto`, `enums`, `exception`, `filter`, `model`, `repository`, `service`, `utils`.
- Every request (except sign-up, login, webhooks and the mock bank) needs `Authorization: Bearer <jwt>`.
  Roles (`CUSTOMER`, `MERCHANT`, `STAFF`) are enforced with `@PreAuthorize`.

---

## Requirement 1 — Create a user & generate account

### Customer sign-up
`POST /api/v1/authentication/signup/customer`

```json
{
  "email": "domtest@gmail.com",
  "username": "domtest",
  "password": "Hello@123",
  "firstName": "dominic",
  "surname": "Ebingonye",
  "phoneNumber": "08060498746"
}
```

One database transaction creates **the user, a wallet and a virtual account**, so a user can never exist without them:

```json
{
  "code": "CUS-KUMH8YUE",
  "username": "domtest",
  "userRoleType": "CUSTOMER",
  "status": "ACTIVE",
  "wallet": {
    "walletNumber": "7435649138",
    "balance": 0.00,
    "currency": "NGN"
  },
  "virtualAccount": {
    "accountNumber": "6918758928",
    "accountName": "RENTAL/DOMINIC EBINGONYE",
    "bankName": "Rental Demo MFB",
    "bankCode": "999",
    "walletNumber": "7435649138"
  }
}
```

- **Wallet number** — 10-digit internal identifier.
- **Virtual account number** — 10-digit **NUBAN-style** number (9-digit serial + check digit computed with the
  CBN weights `3,7,3,3,7,3,…` and bank code `999`). This is the account the user shares to receive bank transfers.
- There is no real payment provider; account numbers are generated in-house.

### Merchant sign-up
`POST /api/v1/authentication/signup/merchant` — same fields plus `businessName`, `businessAddress`, `registrationNumber`.
The merchant gets a wallet and a virtual account (named after the business, e.g. `RENTAL/VINCENT VENTURES`)
but starts as **`PENDING_APPROVAL`** and cannot log in until staff approve them.

### Staff
| Action | Endpoint (staff token required) |
|---|---|
| Create another staff user | `POST /api/v1/authentication/staff` |
| List merchants awaiting approval | `GET /api/v1/authentication/merchants/pending` |
| Approve a merchant | `PUT /api/v1/authentication/merchants/{code}/approve` |
| Suspend / reactivate a user | `PUT /api/v1/authentication/user/{code}/status?status=SUSPENDED` |

### Login (all roles)
`POST /api/v1/authentication/login`

```json
{ "username": "domtest", "password": "Hello@123", "userRoleType": "CUSTOMER" }
```

- `username` accepts the username **or** the email.
- `userRoleType` is optional; when sent, a user of another role is rejected (e.g. a customer on the staff portal).
- Returns `"token": "Bearer eyJ…"`, valid for 1 hour.
- Wrong password, unknown user and wrong portal all return the same `401 Invalid username or password`.

---

## Requirement 2 — Fund transfer between accounts

### Ledger rules (apply to every money movement)
- Money is `BigDecimal` with 2 decimal places, never `double`.
- Every balance change writes a row to `WALLET_TRANSACTIONS` (type, amount, balance before/after, reference),
  so any balance can be explained from its history. A wallet can never go below zero.
- A wallet row is **locked** (`SELECT … WITH (UPDLOCK)`) while its balance changes, so concurrent payments
  cannot overwrite each other or overdraw. When two wallets are involved they are locked **lowest id first**,
  which rules out deadlocks.
- Clients can send a **`reference`**; sending the same one again (e.g. after a timeout) returns the original
  result instead of moving money twice.

### 2a. Bank → wallet (through the virtual account)

This is the realistic flow: the customer sends money from their commercial bank to their virtual account,
and the wallet is credited only after the transfer is **verified**.

Because there is no real bank, a **mock bank** under `/mock-bank` plays the customer's bank and the payment
provider. It talks to the platform only over HTTP, like a real provider.

```
Customer's bank app (mock)        Provider (mock)                    Rental platform
──────────────────────────        ───────────────────                ─────────────────────────────────
1. POST /mock-bank/api/v1/transfers
   → 202, status PENDING
                                  2. ~3 s later: settles
                                     (SUCCESSFUL, or FAILED)
                                  3. POST signed webhook  ─────────▶ 4. verify HMAC-SHA512 signature
                                     (retries with back-off)         5. record event, de-duplicate by session id
                                                         ◀────────── 6. requery provider for the real status
                                                                     7. SUCCESSFUL + our account → credit wallet
```

**Step 1 — send money from the bank app** (no token needed; this simulates the customer's bank):
`POST /mock-bank/api/v1/transfers`

```json
{
  "sourceBankName": "GTBank",
  "sourceAccountNumber": "0123456789",
  "sourceAccountName": "DOMINIC EBINGONYE",
  "destinationAccountNumber": "6918758928",
  "destinationBankCode": "999",
  "amount": 10000.00,
  "narration": "Wallet funding"
}
```

Response `202`:
```json
{ "sessionId": "999058261109142612516322265312", "status": "PENDING", "amount": 10000.00, "destinationAccountName": "RENTAL/DOMINIC EBINGONYE" }
```

**Step 2 — watch it arrive** (customer token): `GET /api/v1/inbound-transfers` shows the status moving
`PENDING_VERIFICATION → CREDITED`, and `GET /api/v1/wallet` shows the new balance.

Why it is built this way:
- **The webhook is only a notification.** Its signature proves it came from the provider, but the wallet is
  credited only after the platform **requeries the provider** (`GET /mock-bank/api/v1/transactions/{sessionId}`
  with the provider secret key). The amount and account come from the requery, not from the webhook body.
- **Re-sent webhooks are harmless.** The bank session id is the ledger reference, so it can only be credited once.
- **The requery happens outside the database transaction**, so no locks are held while waiting on the network.
  If the provider is unreachable the webhook returns `502` and the provider retries later.

Other mock-bank endpoints: `GET /mock-bank/api/v1/name-enquiry?accountNumber=…&bankCode=999` (confirm the
recipient's name) and `GET /mock-bank/api/v1/transfers/{sessionId}` (the customer's receipt).
A source account number ending in **`0000`** always fails with "Insufficient funds", to demonstrate a failed transfer.

### 2b. Direct wallet top-up (simulated)
`POST /api/v1/wallet/fund` (customer) — credits the wallet immediately. This is a shortcut that stands in for a
card payment; the verified path above is the realistic one.

```json
{ "amount": 5000000.00, "reference": "FUND-WALLET-001", "narration": "Wallet funding" }
```

### 2c. Wallet → wallet (customer pays merchant)
| Step | Endpoint (customer token) |
|---|---|
| Confirm the merchant's name | `GET /api/v1/wallet/transfers/recipient?walletNumber=2765874346` |
| Pay | `POST /api/v1/wallet/transfers` |

```json
{ "recipientWalletNumber": "2765874346", "amount": 2500.00, "reference": "PAY-0001", "narration": "Deposit" }
```

In one database transaction: lock both wallets (lowest id first) → check balance → debit customer
(`PAY-0001-DR`) → credit merchant (`PAY-0001-CR`) → save the transfer. Only approved, active merchants can be paid.

`GET /api/v1/wallet/transfers` lists transfers sent (customer) or received (merchant).

### 2d. Renting an asset (wallet-to-wallet payment in a business flow)

**Merchant creates an asset** — `POST /api/v1/assets` (merchant token):
```json
{
  "name": "Honda EU22i Generator",
  "description": "2.2kVA petrol inverter generator",
  "category": "EQUIPMENT",
  "dailyRate": 15000.00,
  "quantity": 3,
  "location": "Lekki Phase 1, Lagos"
}
```
Returns a code such as `AST-6A3W5ZZE`.

**Customer rents it** — `POST /api/v1/rentals` (customer token):
```json
{ "assetCode": "AST-6A3W5ZZE", "quantity": 1, "startDate": "2026-11-10", "endDate": "2026-11-12" }
```
```json
{
  "reference": "RNT-84179E6BDB6F45438E30A26769AFA4FE",
  "days": 3,
  "dailyRate": 15000.00,
  "totalAmount": 45000.00,
  "status": "ACTIVE",
  "merchantName": "Vincent Ventures"
}
```

In one database transaction: lock the asset → check units in stock → compute the price on the server
(daily rate × days × quantity) → lock both wallets → **debit the customer, credit the merchant** →
reduce the asset quantity (it becomes `UNAVAILABLE` at 0) → save the rental. If anything fails (not enough
stock, insufficient balance) nothing is changed.

---

## End-to-end walkthrough

1. `POST /api/v1/authentication/login` as `admin` → staff token
2. `POST /api/v1/authentication/signup/customer` → customer + wallet + virtual account
3. `POST /api/v1/authentication/signup/merchant` → merchant (pending) + wallet + virtual account
4. `PUT /api/v1/authentication/merchants/{code}/approve` with the staff token
5. `POST /mock-bank/api/v1/transfers` to the customer's virtual account → wallet credited after verification
6. Merchant logs in → `POST /api/v1/assets`
7. Customer logs in → `POST /api/v1/rentals` → customer debited, merchant credited
8. Check both sides: `GET /api/v1/wallet`, `GET /api/v1/wallet/transactions`, `GET /api/v1/rentals`

---

## Endpoint reference

| Method | Path | Who |
|---|---|---|
| POST | `/api/v1/authentication/signup/customer` | Public |
| POST | `/api/v1/authentication/signup/merchant` | Public |
| POST | `/api/v1/authentication/login` | Public |
| GET | `/api/v1/authentication/user` | Bearer token |
| POST | `/api/v1/authentication/logout` | Any user |
| POST | `/api/v1/authentication/staff` | Staff |
| GET | `/api/v1/authentication/merchants/pending` | Staff |
| PUT | `/api/v1/authentication/merchants/{code}/approve` | Staff |
| PUT | `/api/v1/authentication/user/{code}/status` | Staff |
| GET | `/api/v1/wallet` | Customer, merchant |
| GET | `/api/v1/wallet/transactions` | Customer, merchant |
| POST | `/api/v1/wallet/fund` | Customer |
| GET | `/api/v1/wallet/transfers/recipient` | Customer |
| POST | `/api/v1/wallet/transfers` | Customer |
| GET | `/api/v1/wallet/transfers`, `/{reference}` | Customer, merchant |
| GET | `/api/v1/virtual-account` | Customer, merchant |
| POST | `/api/v1/virtual-account` | Customer, merchant (create if missing) |
| GET | `/api/v1/inbound-transfers` | Customer, merchant |
| POST | `/api/v1/inbound-transfers/{sessionId}/requery` | Staff |
| POST | `/api/v1/webhooks/bank-transfer` | Payment provider (HMAC signature) |
| POST | `/api/v1/assets` | Merchant |
| GET | `/api/v1/assets/mine` | Merchant |
| GET | `/api/v1/assets/{code}` | Any user |
| POST | `/api/v1/rentals` | Customer |
| GET | `/api/v1/rentals`, `/{reference}` | Customer, merchant |
| GET | `/mock-bank/api/v1/name-enquiry` | Public (simulated bank) |
| POST | `/mock-bank/api/v1/transfers` | Public (simulated bank) |
| GET | `/mock-bank/api/v1/transfers/{sessionId}` | Public (simulated bank) |
| GET | `/mock-bank/api/v1/transactions/{sessionId}` | Provider secret key |

Errors use one shape: `{ "code": "400", "message": "Insufficient wallet balance" }`.

---

## Security notes

- Passwords hashed with **BCrypt**; never returned by the API.
- JWTs are HMAC-signed with a ≥ 256-bit key and the issuer is checked; the user is reloaded on every request,
  so suspending an account takes effect immediately.
- Account status is only revealed after a correct password (no user enumeration).
- Webhook signatures are verified over the **raw body** with a constant-time comparison.
- Secrets (`JWT_SECRET`, `PROVIDER_SECRET_KEY`, `PROVIDER_WEBHOOK_SECRET`, `STAFF_BOOTSTRAP_PASSWORD`) can be
  supplied as environment variables; the values in `application.properties` are local-development defaults only.

---

## Limitations / next steps

- The payment provider is simulated; with a real one (Paystack, Monnify, …) only `PaymentProviderClient`
  and the webhook payload mapping would change.
- The mock bank keeps pending settlements in memory; a restart before settlement leaves that transfer `PENDING`.
- `POST /api/v1/wallet/fund` credits without verification and should be removed once all funding goes through the bank.
- Logout is client-side (tokens stay valid until they expire); refresh tokens / revocation would be next.
- Rentals cannot yet be returned or cancelled (which would refund and restock), and merchants cannot withdraw.
- Automated tests are not included in this submission.
