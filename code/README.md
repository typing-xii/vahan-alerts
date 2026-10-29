# 🚗 Vahan Alerts

### Smart Vehicle Document Management and Expiry Reminder System

Vahan Alerts is a web-based vehicle management application developed using Java and Spring Boot. It helps users manage their vehicles, maintain document records, track vehicle servicing, and receive timely notifications before important vehicle documents expire.

The application provides a centralized platform for vehicle owners to maintain their vehicle information and monitor document expiry dates without relying on paper records or manual reminders.

---

## 📌 Table of Contents

- [Project Overview](#-project-overview)
- [Objectives](#-objectives)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Application Architecture](#-application-architecture)
- [Document Expiry Management](#-document-expiry-management)
- [Notification System](#-notification-system)
- [Security and Data Protection](#-security-and-data-protection)
- [Database Management](#-database-management)
- [Prerequisites](#-prerequisites)
- [Installation and Setup](#-installation-and-setup)
- [Running the Application](#-running-the-application)
- [Running Tests](#-running-tests)
- [Project Structure](#-project-structure)
- [Current Limitations](#-current-limitations)
- [Future Enhancements](#-future-enhancements)
- [Academic Project Note](#-academic-project-note)

---

## 📖 Project Overview

Vehicle owners must keep track of several important documents, including vehicle registration certificates, insurance policies, Pollution Under Control (PUC) certificates, permits, and fitness certificates.

Forgetting to renew these documents before their expiry dates can create inconvenience and potential legal or compliance issues.

Vahan Alerts addresses this problem by allowing users to register their vehicles, manually enter document details, record maintenance activities, and receive automated in-app notifications for upcoming document expirations.

The application is designed with a focus on simplicity, security, data accuracy, and ease of use.

## 🎯 Objectives

- Provide a centralized system for managing multiple vehicles.
- Maintain digital records of vehicle-related documents through manual data entry.
- Monitor document expiry dates automatically.
- Generate timely in-app notifications.
- Maintain vehicle maintenance and service history.
- Associate every document and service record with the correct vehicle.
- Protect user data through authentication and ownership-based authorization.
- Provide a foundation for future messaging and installable application features.

---

## ✨ Features

### 1. User Registration and Authentication

- User registration with personal details.
- Phone-number-based login.
- Password protection using BCrypt hashing.
- Date-of-birth selection during registration.
- WhatsApp reminder preference available during registration.
- Session-based authentication and protected application routes.

### 2. Vehicle Management

Users can manage their own vehicles through the **My Vehicles** section.

Features include:

- Adding vehicle records.
- Viewing registered vehicles.
- Accessing individual vehicle details.
- Editing or managing vehicle information through the existing application functionality.
- Keeping vehicle-related records associated with the appropriate vehicle.

The application supports multiple vehicles per user.

### 3. Vehicle Document Management

Users manually enter and maintain document information without uploading photographs or PDF files.

Supported document categories include:

- Insurance
- Pollution Under Control (PUC)
- Registration Certificate (RC)
- Fitness Certificate
- Permit
- Driving Licence
- Other

Document information can include:

- Document type
- Certificate or document number
- Issue date
- Expiry date, where applicable
- Notes

Each document is associated with a selected vehicle belonging to the logged-in user.

### 4. Document Expiry Tracking

The application calculates document status using expiry dates.

| Status | Meaning |
|---|---|
| Expired | The expiry date has passed. |
| Expiring Soon | The document expires today or within the next 30 days. |
| Valid | More than 30 days remain before expiry. |
| No Expiry | The document does not require an expiry date. |

These statuses help users identify documents that require attention.

### 5. Maintenance and Service History

Users can maintain service records for their vehicles.

Available information includes:

- Service date
- Odometer reading
- Service type
- Workshop or service centre
- Service cost
- Additional notes

The application supports viewing service history, with recent records displayed first.

It also provides service-related summary information, including service counts, total recorded costs, and the latest service date where available.

Validation helps maintain sensible service dates and odometer chronology.

### 6. Automated In-App Notifications

Vahan Alerts includes an automated document-expiry reminder system.

The current reminder schedule uses the following offsets:

- 30 days before expiry
- 7 days before expiry
- 1 day before expiry
- On the expiry date

The reminder scheduler runs daily, with the default configured schedule at 09:00 in the Asia/Kolkata time zone.

The system also processes eligible reminders at application startup and after relevant document creation or updates, helping recover reminders that may otherwise be missed between scheduled runs.

### 7. Notification Centre

The Notifications section allows users to:

- View generated reminders.
- Identify the associated document and vehicle.
- See the relevant vehicle registration number.
- Review unread notifications.
- Mark individual notifications as read.
- Mark all notifications as read.

Duplicate prevention helps avoid creating the same reminder repeatedly for the same document expiry snapshot and reminder offset.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java JDK 25 | Application programming language and runtime |
| Spring Boot | Application framework |
| Spring MVC | HTTP request handling and controller layer |
| Thymeleaf | Server-side HTML template rendering |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Database access and entity management |
| Hibernate | ORM implementation |
| H2 Database | Persistent relational storage |
| Flyway | Version-controlled database migrations |
| Maven Wrapper | Dependency management, building and testing |
| HTML5 | Page structure |
| CSS3 | Styling and responsive presentation |
| JavaScript | Client-side interactions where required |
| JUnit and Spring Boot Test | Automated testing |

---

## 🏗️ Application Architecture

Vahan Alerts follows a layered Spring Boot architecture.

```text
                    User / Browser
                          |
                          v
                 Thymeleaf Web Pages
                          |
                          v
                 Spring MVC Controllers
                          |
                          v
                    Service Layer
              Business Logic and Validation
                          |
                          v
                Spring Data JPA / Hibernate
                          |
                          v
                    H2 Database
                          ^
                          |
                   Flyway Migrations

        Scheduled Expiry Reminder Processing
                          |
                          v
                 In-App Notifications
```

### Architecture Components

**Controller Layer**

Receives HTTP requests, handles form submissions, and coordinates responses.

**Service Layer**

Contains business logic for vehicle management, document operations, maintenance records, and notification processing.

**Repository and Persistence Layer**

Uses Spring Data JPA and Hibernate to interact with the relational database.

**Template Layer**

Uses Thymeleaf to render the user interface and display application data.

**Security Layer**

Restricts protected functionality to authenticated users and enforces ownership rules.

**Scheduled Processing**

Checks eligible document-expiry reminders and generates notifications according to the configured schedule.

---

## ⏰ Document Expiry Management

The reminder system evaluates document expiry dates and determines which reminder offsets are eligible.

For example, for a document expiring on a particular date:

| Reminder | Trigger |
|---|---|
| First reminder | 30 days before expiry |
| Second reminder | 7 days before expiry |
| Third reminder | 1 day before expiry |
| Final reminder | On the expiry date |

The system records reminders against the document's expiry-date snapshot and offset. This helps prevent duplicates while allowing a renewed document with a new expiry date to have its own reminder schedule.

Notifications retain relevant information about the document and vehicle so users can identify which record requires attention.

---

## 🔔 Notification System

The current notification implementation is **in-app only**.

When a document reaches an eligible reminder date, the application generates a notification that can be viewed in the notification centre.

The reminder processing system is designed to:

- Prevent duplicate reminder creation.
- Associate reminders with the correct document and vehicle.
- Respect user ownership boundaries.
- Preserve notification history.
- Recover eligible reminders during startup and document updates.
- Continue generating in-app notifications independently of external messaging services.

Actual WhatsApp, SMS, email, and push notifications are not currently implemented.

---

## 🔐 Security and Data Protection

Security is an important part of Vahan Alerts.

### Authentication

Users must authenticate before accessing protected application features.

### Password Security

Passwords are hashed using BCrypt rather than being stored as plain text.

### Ownership-Based Authorization

Users may access only their own vehicles and the associated documents, service records, and notifications.

### Input Validation

The application validates submitted data and applies business rules to vehicle documents and maintenance records.

### CSRF Protection

State-changing web requests use Spring Security's CSRF protections.

### Private Data Handling

The application uses manual document data entry and does not currently require document scans or uploaded files.

API credentials and other secrets should be supplied through secure configuration rather than committed to source control.

---

## 🗄️ Database Management

Vahan Alerts uses an H2 relational database configured for persistent storage.

JPA entities represent application records, while repositories handle database operations.

Flyway manages schema changes through ordered SQL migrations.

This approach allows the database schema to evolve while preserving existing records when migrations are applied correctly.

**Important:** The application uses a file-backed database. Keep the database files out of public repositories and back up the database before making destructive database or migration changes.

Do not delete the existing database files when updating or running the application.

---

## 💻 Prerequisites

Before running the application, install:

1. Java Development Kit (JDK) 25.
2. Git, if you want to clone the repository.
3. A code editor such as Visual Studio Code.

Maven does not need to be installed globally because the project includes the Maven Wrapper.

Verify Java installation:

```cmd
java -version
```

Verify Git installation if needed:

```cmd
git --version
```

---

## 🚀 Installation and Setup

### Step 1: Clone the Repository

Replace `YOUR_USERNAME` with the GitHub account or organization that owns the repository.

```cmd
git clone https://github.com/typing-xii/Vahan_Alerts.git
```

### Step 2: Open the Project

```cmd
cd Vahan_Alerts
```

Open the project folder in Visual Studio Code or your preferred Java IDE.

### Step 3: Configure the Environment

Review the application's configuration files before starting it.

Ensure that:

- The Java version is compatible with the project.
- The configured database path is writable.
- Required application settings are available.
- Sensitive credentials are not stored in public source files.

Use the existing application configuration rather than replacing it with a new database configuration.

### Step 4: Build the Project

On Windows Command Prompt:

```cmd
mvnw.cmd clean package
```

This command compiles the application, runs the configured tests, and packages the application if the build succeeds.

---

## ▶️ Running the Application

From the project root, run:

```cmd
mvnw.cmd spring-boot:run
```

Once Spring Boot starts successfully, open:

http://localhost:8080

Register an account or sign in with an existing account.

The application can then be used to manage vehicles, enter document information, maintain service history, and view notifications.

Keep the application running for its scheduled reminder processing to execute.

---

## 🧪 Running Tests

Run the complete automated test suite:

```cmd
mvnw.cmd test
```

Run a specific test class:

```cmd
mvnw.cmd -Dtest=VehicleTests test
```

Replace `VehicleTests` with the relevant test class name when testing another feature.

The test suite covers application behavior across the implemented features. The latest reported test run before this README was prepared completed with 56 tests passing, with zero failures, errors, or skipped tests. Run the tests again against your current checkout to verify its state.

---

## 📁 Project Structure

The application follows a conventional Spring Boot project layout.

```text
Vahan_Alerts/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/vahanalerts/
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       └── ...
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── vehicles/
│   │       │   └── ...
│   │       ├── static/
│   │       ├── db/migration/
│   │       └── application.properties
│   └── test/
│       └── java/
├── data/                 # Local runtime data, if configured here
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

The exact package and resource structure should be checked against the current repository. The diagram above shows the intended architectural organization, not a guarantee that every listed package exists.

---

## ⚠️ Current Limitations

- Real WhatsApp messages are not currently sent.
- SMS, email and push delivery are not currently implemented.
- The application must be running for its scheduled reminder processing to execute.
- The existing H2 database is intended for the current project setup; production deployment would require appropriate database, backup, and operational planning.
- The application has not yet been converted into an installable Progressive Web App (PWA).

---

## 🔮 Future Enhancements

Potential future improvements include:

- Official WhatsApp Business Cloud API integration.
- Configurable reminder schedules by document type.
- A WhatsApp message preview and academic demonstration mode.
- An installable Progressive Web App (PWA).
- Improved mobile application experience.
- Additional reporting and document-management features.
- Production database deployment and operational monitoring.

These are planned enhancements and should not be considered implemented until they are completed and tested.

---

## 🎓 Academic Project Note

Vahan Alerts is an academic software development project demonstrating web application development using Java and Spring Boot.

It incorporates authentication, relational data persistence, validation, vehicle and maintenance management, scheduled background processing, database migrations, and in-app notifications.

The project can be extended with external messaging integrations and application-installation features as future development work.

---

## 📄 License

No open-source license has been specified yet. If you intend to distribute the project publicly, choose and add an appropriate license before representing the repository as open source.

---

**Vahan Alerts — Manage Your Vehicles. Track Your Documents. Stay Informed.**
