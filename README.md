# TUKAC Portal — Technical University of Kenya Ability Club

Welcome to the **TUKAC Portal**, a specialized digital ecosystem built to support and empower students with disabilities at TUK. This project integrates community engagement, event management, and financial transparency into a single, secure platform.

---

## 🚀 Key Features
- **Secure Authentication:** RBAC (Role-Based Access Control) using JWT and BCrypt hashing.
- **Financial Transparency:** Real-time tracking of club income and expenses with precise balance calculations.
- **Event Management:** Interactive calendar with RSVP functionality and attendee tracking.
- **Audit Logging:** Continuous monitoring of system activities and security events.
- **Professional Reporting:** On-demand PDF generation for membership and financial records using JasperReports.
- **Inclusive Design:** A high-performance web interface following modern accessibility and aesthetic standards.

## 🛠️ Technology Stack
- **Backend:** Spring Boot 3.2.5 (Java 17)
- **Database:** SQLite (Relational, Embedded)
- **Frontend:** Vanilla HTML5, CSS3 (Glassmorphism), and JavaScript (ES6)
- **Security:** Spring Security & JWT
- **Reporting:** JasperReports Engine

## 📂 Project Structure
- `/tukac-web`: Main web portal module (Spring Boot backend & static frontend).
- `/tukac-portal`: Shared library/legacy module for desktop integration.
- `/src/main/resources/static`: Responsive web frontend.
- `/src/main/resources/reports`: JasperReport `.jrxml` templates.

## ⚙️ Installation & Setup

### 1. Prerequisites
- Java JDK 17 or higher.
- Maven 3.x.

### 2. Running the Application
1. Clone the repository to your local machine.
2. Navigate to the root directory.
3. Run the following command:
   ```bash
   mvn spring-boot:run -pl tukac-web
   ```
4. Open your browser and navigate to: `http://localhost:8080`

### 3. Database
The system uses an embedded SQLite database (`tukac.db`). This file is automatically created and managed in the root directory. No external database server (like MySQL) is required.

## 👥 Default Credentials (For Testing)
| Role | Email | Password |
| :--- | :--- | :--- |
| **Chairperson** | chairperson@tuk.ac.ke | password123 |
| **Treasurer** | treasurer@tuk.ac.ke | password123 |
| **Member** | member@tuk.ac.ke | password123 |

---

## 📄 License
This project is for academic evaluation for the Technical University of Kenya. All rights reserved © 2026.
