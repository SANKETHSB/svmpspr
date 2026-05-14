# Smart Vendor & Procurement Management System (SVPMS)
**Infosys Limited | Java Spring Boot + React + TypeScript + MySQL**

---

## 🚀 Quick Start

### Prerequisites
- Java 17+, Maven 3.9+
- Node.js 18+, npm 9+
- MySQL 8.0+

### 1. Database Setup
```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```
Or let Spring Boot auto-create tables (JPA DDL is set to `update`).

### 2. Configure Database
Edit `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/svpms_db
spring.datasource.username=root
spring.datasource.password=root
```

### 3. Run Backend
```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```
Backend starts at: **http://localhost:8080/api**
Swagger UI: **http://localhost:8080/api/swagger-ui.html**

### 4. Run Frontend
```bash
cd frontend
npm install
npm start
```
Frontend starts at: **http://localhost:3000**

---

## 🔑 Default Login Credentials

| Role                  | Email                       | Password          |
|-----------------------|-----------------------------|-------------------|
| Admin                 | admin@svpms.com             | Admin@123456      |
| Procurement Manager   | manager@svpms.com           | Manager@123456    |
| Compliance Officer    | compliance@svpms.com        | Compliance@123456 |
| Vendor (pre-approved) | vendor@techsupply.com       | Vendor@123456     |

> Vendors can also self-register at `/register`

---

## 🗂️ Project Structure

```
svpms/
├── backend/                          # Spring Boot 3.2 (Java 17)
│   └── src/main/java/com/infosys/svpms/
│       ├── config/          SecurityConfig, AppConfig (Swagger, ModelMapper)
│       ├── controller/      Auth, User, Vendor, RFQ, Quotation, PO, Notifications, Audit, Dashboard
│       ├── dto/request/     LoginRequest, UserRequest, VendorRegisterRequest, RfqRequest,
│       │                    QuotationRequest, PORequest, VendorApprovalRequest, QuotationEvaluationRequest
│       ├── dto/response/    ApiResponse, LoginResponse, UserResponse, VendorResponse, RfqResponse,
│       │                    QuotationResponse, POResponse, ComplianceDocResponse, AuditLogResponse,
│       │                    NotificationResponse, DashboardResponse
│       ├── entity/          User, Vendor, ComplianceDocument, RFQ, RfqItem, RfqVendorInvite,
│       │                    Quotation, QuotationItem, PurchaseOrder, AuditLog, Notification
│       ├── exception/       GlobalExceptionHandler, ResourceNotFoundException, BusinessException,
│       │                    DuplicateException
│       ├── repository/      11 Spring Data JPA repositories with custom JPQL
│       ├── scheduler/       SvpmsScheduler (RFQ auto-close, compliance expiry)
│       ├── security/        JwtUtil, JwtFilter, CombinedUserDetailsService
│       ├── service/impl/    AuthServiceImpl, UserServiceImpl, VendorServiceImpl,
│       │                    ComplianceDocumentServiceImpl, RfqServiceImpl, QuotationServiceImpl,
│       │                    PurchaseOrderServiceImpl, DashboardServiceImpl,
│       │                    AuditServiceImpl, NotificationServiceImpl
│       └── utility/         DataSeeder (auto-seeds default users on startup)
│
└── frontend/                         # React 18 + TypeScript + Bootstrap 5
    └── src/
        ├── App.tsx                   # Routes + Role-based Guards
        ├── App.css                   # Complete dark/light CSS variable system
        ├── context/                  AuthContext.tsx, ThemeContext.tsx
        ├── services/api.ts           # All Axios API calls
        ├── types/index.ts            # TypeScript interfaces
        ├── components/
        │   ├── common/               StatusBadge, Spinner, EmptyState, Pagination,
        │   │                         ConfirmModal, Avatar, InfoRow, PageHeader, fmt helpers
        │   └── layout/               Layout.tsx (sidebar + topbar + theme toggle)
        └── pages/
            ├── auth/                 LoginPage, RegisterVendorPage
            ├── dashboard/            DashboardPage (KPI cards + Doughnut/Bar charts)
            ├── vendors/              VendorListPage, VendorDetailPage
            ├── rfqs/                 RfqListPage, RfqCreatePage, RfqDetailPage
            ├── quotations/           QuotationListPage, QuotationSubmitPage, QuotationComparePage
            ├── purchase-orders/      POListPage, POCreatePage, PODetailPage
            ├── notifications/        NotificationsPage
            ├── audit/                AuditLogPage
            └── admin/                UserManagementPage
```

---

## 🌙 Dark / Light Theme

Toggle is in the **top-right of the topbar**. Uses CSS custom properties (`--bg`, `--surface`, `--text`, etc.) applied via `data-theme` attribute on `<html>`. Preference is persisted in `localStorage`.

---

## 🔒 Security

- JWT Bearer authentication (HS256)
- Role-based access: `ADMIN`, `PROCUREMENT_MANAGER`, `COMPLIANCE_OFFICER`, `VENDOR`
- Account lockout after 5 failed logins (30 min)
- BCrypt password hashing (strength 10)
- Immutable audit log for all critical actions

---

## ⚙️ Scheduled Jobs

| Job | Schedule | Description |
|-----|----------|-------------|
| RFQ Auto-Close | Every hour | Closes OPEN RFQs past their deadline |
| Compliance Expiry | Daily 8 AM | Marks expired docs, warns vendors 30 days before |

---

## 📡 API Endpoints

| Module | Base URL | Methods |
|--------|----------|---------|
| Auth | `/api/auth` | POST login, POST logout |
| Users | `/api/users` | GET, POST, PUT, DELETE, PATCH unlock |
| Vendors | `/api/vendors` | GET, POST register, PATCH approve/reject/suspend |
| RFQs | `/api/rfqs` | GET, POST, PUT, PATCH close/award |
| Quotations | `/api/quotations` | GET, POST, PUT, POST evaluate |
| Purchase Orders | `/api/purchase-orders/rfq/{rfqId}` | POST generate, GET, PATCH status, GET export PDF |
| Notifications | `/api/notifications` | GET, PATCH read/read-all |
| Audit Logs | `/api/audit-logs` | GET, GET export CSV |
| Dashboard | `/api/dashboard/kpis` | GET |

Full docs at: **http://localhost:8080/api/swagger-ui.html**

---

## 🧪 Running Tests

```bash
cd backend
mvn test
```

---

## 📦 Production Build

```bash
# Frontend build
cd frontend && npm run build

# Backend JAR
cd backend && mvn clean package -DskipTests
java -jar target/svpms-1.0.0.jar
```
