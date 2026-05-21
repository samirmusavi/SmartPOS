# SmartPOS — UAE Retail Management Platform

A cloud-ready SaaS point-of-sale and retail management system built for UAE small businesses.

Built by **Mohamad Wasim Rezma** & **Akhmad Samir Mossavi**
University of Sharjah — Junior Project — Spring 2026

---

## Features

- **Point of Sale** — Complete sales in under 10 seconds. VAT auto-calculated. Unique traceable receipts.
- **Multi-Branch Inventory** — Real-time stock per branch. Transfers, adjustments, archiving.
- **Stockout Intelligence** — Days-until-empty predictions. Auto-drafted purchase orders.
- **Financial Reports** — P&L statement, revenue by branch/category, VAT export.
- **Multi-Branch Dashboard** — Owner sees all branches. Full data isolation between branches.
- **Arabic / RTL** — Full right-to-left layout. One click, no page reload.
- **Offline PWA** — Works without internet. Sales sync automatically on reconnect.
- **Staff & Customers** — Role-based accounts, loyalty tiers, supplier management.
- **Admin Portal** — SaaS operator dashboard: MRR, all subscribed businesses, plan management.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Security, JPA/Hibernate
- **Database:** MySQL 8
- **Frontend:** Vanilla HTML/CSS/JS, Progressive Web App (PWA)
- **Auth:** JWT tokens, BCrypt password hashing

---

## Setup Instructions

### 1. Prerequisites
- Java 17+
- MySQL 8+
- Maven (or use the included `mvnw` wrapper)

### 2. Database
Create a MySQL database:
```sql
CREATE DATABASE smartpos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Then run the seed file to create the admin account:
```
mysql -u root -p smartpos_db < seed.sql
```

### 3. Configuration
Open `src/main/resources/application.properties` and replace:
```
YOUR_MYSQL_USERNAME   → your MySQL username
YOUR_MYSQL_PASSWORD   → your MySQL password
YOUR_EMAIL@gmail.com  → your Gmail address
YOUR_APP_PASSWORD     → your Gmail App Password
YOUR_JWT_SECRET_KEY   → any random string (min 32 characters)
```

### 4. Run
```bash
./mvnw spring-boot:run
```
Then open: [http://localhost:8090](http://localhost:8090)

---

## Project Structure

```
src/
├── main/
│   ├── java/com/business/managementsystem/
│   │   ├── config/        # Security, CORS, migration
│   │   ├── controller/    # REST API endpoints
│   │   ├── dto/           # Data transfer objects
│   │   ├── model/         # JPA entities
│   │   ├── repository/    # Spring Data repositories
│   │   ├── security/      # JWT filter & utilities
│   │   └── service/       # Business logic
│   └── resources/
│       ├── static/        # Frontend (HTML, CSS, JS)
│       └── application.properties
└── test/
```

---

## License

University of Sharjah — Academic Project — Spring 2026
