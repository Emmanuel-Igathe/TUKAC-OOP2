# TUKAC Portal - Database Design & Normalization

This document outlines the database architecture for the TUK Ability Club (TUKAC) Portal, specifically designed to meet the academic requirements for the OOP/Database project.

## 📊 1. Database Identification
**Engine:** SQLite (Relational Database Management System)
**File:** `tukac.db`

---

## 🏗️ 2. Entity-Relationship Schema (Normalized to 3NF)

### 👤 Entity: Users (`users` table)
Stores member profiles, credentials, and disability information.
*   **ID** (PK): BIGINT, Auto-increment.
*   **Student_ID** (Unique): VARCHAR, e.g., C022-01-1234/2023.
*   **Name**: VARCHAR, Full name.
*   **Email** (Unique): VARCHAR.
*   **Password**: VARCHAR (BCrypt Hashed for security).
*   **Role**: VARCHAR (CHAIRPERSON, TREASURER, SECRETARY, MEMBER).
*   **Is_Approved**: INTEGER (0 for pending, 1 for approved).
*   **Has_Disability**: BOOLEAN.
*   **Disability_Type**: VARCHAR (Optional).
*   **NCPWD_Number**: VARCHAR (Optional).
*   **Passport_Photo**: TEXT (Base64 encoded).

### 📅 Entity: Events (`events` table)
Manages club activities and scheduling.
*   **ID** (PK): BIGINT, Auto-increment.
*   **Title**: VARCHAR.
*   **Description**: TEXT.
*   **Event_Date**: DATE.
*   **Event_Time**: TIME.
*   **Location**: VARCHAR.
*   **Capacity**: INTEGER.
*   **Created_By** (FK): BIGINT -> `users.id`.

### 💰 Entity: Transactions (`transactions` table)
Records all financial activities (Income/Expenses).
*   **ID** (PK): BIGINT, Auto-increment.
*   **Type**: VARCHAR (income, expense).
*   **Amount**: DOUBLE.
*   **Description**: VARCHAR.
*   **Category**: VARCHAR (Donations, Dues, Equipment, etc.).
*   **Transaction_Date**: DATE.
*   **Created_By** (FK): BIGINT -> `users.id`.

### ✍️ Entity: Blog Posts (`blog_posts` table)
Stores announcements and club news.
*   **ID** (PK): BIGINT, Auto-increment.
*   **Title**: VARCHAR.
*   **Content**: TEXT.
*   **Category**: VARCHAR.
*   **Media_Url**: VARCHAR (Path to image/video).
*   **Author_Id** (FK): BIGINT -> `users.id`.
*   **Published_At**: TIMESTAMP.

### 💬 Entity: Forum Posts (`forum_posts` table)
Manages student-led discussions.
*   **ID** (PK): BIGINT, Auto-increment.
*   **Title**: VARCHAR.
*   **Content**: TEXT.
*   **Author_Id** (FK): BIGINT -> `users.id`.
*   **Created_At**: TIMESTAMP.

---

## 🔗 3. Relationships & Cardinality

1.  **Users ↔ Events (1:N):** One User (Executive) can create many Events. Each event is linked to its creator via `created_by`.
2.  **Users ↔ Blog Posts (1:N):** One User (Secretary) can publish many articles. Linked via `author_id`.
3.  **Users ↔ Transactions (1:N):** One User (Treasurer) can record many transactions. Linked via `created_by`.
4.  **Users ↔ Forum Posts (1:N):** One Member can start many discussions. Linked via `author_id`.

---

## 🛡️ 4. Security & Best Practices
*   **Password Hashing:** All passwords use the **BCrypt** hashing algorithm.
*   **Prepared Statements:** All database queries are handled by Spring Data JPA, which uses prepared statements to prevent **SQL Injection**.
*   **Normalization:** The schema is fully normalized to 3NF to eliminate data redundancy and ensure referential integrity.
*   **Audit Trail:** The `activity_logs` table tracks every login, registration, and data modification with IP addresses for accountability.
