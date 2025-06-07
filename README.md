# Hospital Management System - DEV6003 Secure Application

## 🎯 Assessment 002 Implementation
This is a secure hospital patient management system built for DEV6003 Assessment 002, implementing comprehensive security features on top of the original Spring Boot application.

## 🔒 Security Features Implemented

1. **JWT Authentication** ✅ - Stateless REST API security
2. **Role-Based Access Control** ✅ - 4 roles: ADMIN, DOCTOR, SECRETARIAT, PATIENT
3. **Password Security** ✅ - BCrypt encryption, complexity validation, change policy
4. **Diagnosis Encryption** ✅ - AES-256 for medical data protection
5. **Input Validation** ✅ - Protection against SQL injection and XSS
6. **Audit Logging** ✅ - Comprehensive security event logging
7. **Account Lockout** ✅ - After 3 failed login attempts

## 🚀 Quick Start

### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.6+

### 1. Database Setup
```bash
# Create database
mysql -u root -p
CREATE DATABASE nycsecdb;
exit;

# Update application.properties with your MySQL credentials
# Default: username=root, password=root123
```

### 2. Run Application
```bash
# Clone and enter directory
cd DEV6003FinalProject

# Build and run
mvn clean compile spring-boot:run

# The app will:
# - Start on http://localhost:8080
# - Create tables automatically
# - Import initial data from import.sql
```

### 3. Test Login
```bash
# Login and get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"1234"}'

# Response will include JWT token for authenticated requests
```

## 👥 Default Users & Credentials

| Username  | Password | Role             | Permissions |
|-----------|----------|------------------|-------------|
| admin     | 1234     | ROLE_ADMIN       | Full system access |
| doctor    | 1234     | ROLE_DOCTOR      | Create visits, view all patients, edit own visits |
| secretary | 1234     | ROLE_SECRETARIAT | Manage users and patients, no medical data |
| patient   | 1234     | ROLE_PATIENT     | View own data only |

## 📁 Project Structure

```
src/main/java/com/nyc/hosp/
├── security/              # JWT & Spring Security configuration
│   ├── SecurityConfig.java
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── auth/                  # Authentication endpoints
│   ├── AuthController.java
│   └── dto/
├── encryption/            # Medical data encryption
│   └── DiagnosisEncryptionService.java
├── validation/            # Input validation & sanitization
│   ├── InputSanitizer.java
│   └── PasswordValidator.java
├── audit/                 # Security audit logging
│   └── AuditLogService.java
└── domain/               # Enhanced entities
    ├── Hospuser.java     # Added security fields
    ├── Role.java         # Enum-based roles
    └── Patientvisit.java # Added encrypted diagnosis
```

## 🔐 Security Implementation Details

### Password Policy
- Minimum 8 characters
- Must contain: uppercase, lowercase, digit, special character
- Expires after 90 days
- BCrypt encoding (strength 12)

### JWT Configuration
- Algorithm: HS512
- Token validity: 24 hours
- Secret key: 512+ bits (secure)
- Stateless authentication

### Diagnosis Encryption
- Algorithm: AES-256-GCM
- Each diagnosis encrypted before storage
- Automatic decryption for authorized users

### Input Validation
- All DTOs use Bean Validation
- SQL injection prevention
- XSS protection through sanitization
- Request size limits

## 📝 API Documentation

### Authentication Endpoints

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "admin",
  "password": "1234"
}

# Response:
{
  "accessToken": "eyJhbGciOiJIUzUxMi...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin",
  "email": "admin@admin.com",
  "roles": ["ROLE_ADMIN"]
}
```

#### Change Password
```bash
POST /api/auth/change-password
Authorization: Bearer {token}
Content-Type: application/json

{
  "oldPassword": "1234",
  "newPassword": "NewPass123!"
}
```

### Protected Endpoints (require JWT)

#### Admin Only
- `GET /api/admin/**` - Admin dashboard
- `GET /api/audit/**` - Audit logs

#### Doctor Access
- `POST /api/visits/create` - Create patient visit
- `PUT /api/visits/{id}` - Update own visits only
- `GET /api/patients/**` - View all patients

#### Secretary Access
- `GET/POST/PUT /api/users/**` - Manage users
- `GET/POST/PUT /api/patients/**` - Manage patients
- No access to medical diagnoses

## 🧪 Testing Guide

### 1. Test Authentication
```bash
# Test successful login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"1234"}'

# Test failed login (wrong password)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"wrong"}'

# After 3 failed attempts, account locks
```

### 2. Test Role-Based Access
```bash
# Get JWT token for doctor
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"doctor","password":"1234"}' | jq -r '.accessToken')

# Try to access admin endpoint (should fail)
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Test Input Validation
```bash
# Test SQL injection attempt (should be blocked)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin'; DROP TABLE users;--","password":"1234"}'
```

## 🐛 Troubleshooting

### Common Issues

1. **Login returns 401 Unauthorized**
   - Check if JWT secret is properly configured (must be 512+ bits)
   - Verify /api/auth/login is in public endpoints

2. **Invalid username or password**
   - Verify BCrypt hash in database matches password
   - Check if account is locked (failed_login_attempts >= 3)

3. **Database connection issues**
   - Verify MySQL is running: `sudo service mysql status`
   - Check credentials in application.properties
   - Ensure database 'nycsecdb' exists

### Reset Failed Login Attempts
```sql
UPDATE hospuser SET failed_login_attempts = 0, account_locked = false WHERE username = 'admin';
```

## 📊 Database Schema

### Enhanced Tables
- `hospuser` - Added: password, email, lastPasswordChange, failedLoginAttempts, accountLocked
- `roles` - Changed to enum: ROLE_ADMIN, ROLE_DOCTOR, ROLE_SECRETARIAT, ROLE_PATIENT
- `patientvisit` - Added: encryptedDiagnosis
- `user_roles` - Many-to-many user-role mapping
- `audit_log` - Security event logging

## ✅ Assessment Requirements Coverage

| Requirement | Implementation | Status |
|-------------|----------------|---------|
| Validate all user input | Bean Validation, Input Sanitizer | ✅ |
| Password complexity | PasswordValidator with rules | ✅ |
| Change password policy | 90-day expiry, change endpoint | ✅ |
| 4 user roles | Admin, Doctor, Secretariat, Patient | ✅ |
| Role-based access | Spring Security + @PreAuthorize | ✅ |
| Encrypted diagnosis | AES-256-GCM encryption | ✅ |
| JWT REST API | Stateless authentication | ✅ |
| Authentication | Spring Security + JWT | ✅ |
| Authorization | Role-based method security | ✅ |
| Logging | Comprehensive audit logging | ✅ |
| Session management | Stateless (JWT) | ✅ |
| Injection prevention | Parameterized queries, validation | ✅ |
| CSRF protection | Disabled for REST API | ✅ |

## 🔗 Resources

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [JWT Introduction](https://jwt.io/introduction/)
- [OWASP Security Guidelines](https://owasp.org/www-project-top-ten/)
- [BCrypt Calculator](https://bcrypt-generator.com/)

## 📄 License

This project is created for educational purposes as part of DEV6003 coursework.
