# Hospital Management System - DEV6003 Secure Application Development

## 🏥 Project Overview

A secure hospital patient management system developed for DEV6003 Assessment 002, implementing comprehensive security features for managing patient visits and medical data.

### 🎯 Assessment Requirements Met

1. ✅ **Input Validation** - All user inputs validated against SQL injection and XSS attacks
2. ✅ **Password Policy** - Complex password requirements with 90-day change policy
3. ✅ **Role-Based Access Control** - 4 roles: ADMIN, DOCTOR, SECRETARIAT, PATIENT
4. ✅ **Diagnosis Encryption** - AES-256 encryption for sensitive medical data
5. ✅ **JWT Security** - Stateless authentication for REST API endpoints
6. ✅ **All Security Techniques** - Authentication, authorization, logging, session management, CSRF protection

## 🚀 Quick Start

### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.6+

### Database Setup
```sql
CREATE DATABASE nycsecdb;
USE nycsecdb;
```

### Running the Application
```bash
# Clone the repository
git clone https://github.com/yourusername/DEV6003FinalProject.git
cd DEV6003_Projects

# Create SSL certificate (first time only)
./scripts/security/create_ssl_certificate.sh

# Build and run
mvn clean compile
mvn spring-boot:run
```

### 🎯 Assessment Testing Steps

1. **Access the Application**
   - Navigate to: https://localhost:8443
   - Accept the self-signed certificate warning

2. **Login with Initial Credentials**
   - Username: admin
   - Password: 1234

3. **Change Password (REQUIRED)**
   - Go to User Menu → Change Password
   - Follow password requirements shown on screen
     - Use secure password from NEW_CREDENTIALS.md
4. **Test Different Roles**
   - Logout and login with different users
   - Verify role-based permissions

### Default Users (MUST CHANGE ON FIRST LOGIN!)
| Username | Initial Password | Role | New Secure Password |
|----------|-----------------|------|---------------------|
| admin | 1234 | ADMIN | See docs/setup/NEW_CREDENTIALS.md |
| george | 1234 | DOCTOR | See docs/setup/NEW_CREDENTIALS.md |
| secretary | 1234 | SECRETARIAT | See docs/setup/NEW_CREDENTIALS.md |
| patient1 | 1234 | PATIENT | See docs/setup/NEW_CREDENTIALS.md |

⚠️ **IMPORTANT**: All users must change their password on first login using the "Change Password" feature!

## 🔒 Security Features

### Authentication & Authorization
- **Dual Authentication System**:
  - Session-based for web interface (Thymeleaf)
  - JWT-based for REST API endpoints
- **Spring Security 6.x** with method-level security
- **Account lockout** after 3 failed login attempts

### Data Protection
- **AES-256 Encryption** for medical diagnoses
- **BCrypt** password hashing (strength 12)
- **Input sanitization** preventing SQL injection and XSS
- **CSRF protection** for all web forms

### Access Control
- **ADMIN**: Full system access
- **DOCTOR**: Create/edit own visits, view all patients
- **SECRETARIAT**: Manage patients and doctors only
- **PATIENT**: View own medical records

## 📁 Project Structure
```
src/main/java/com/nyc/hosp/
├── controller/         # REST and Web controllers
├── security/          # Security configuration and JWT
├── service/           # Business logic layer
├── repository/        # Data access layer
├── domain/            # Entity classes
├── dto/              # Data transfer objects
├── encryption/        # AES encryption service
├── validation/        # Input validation utilities
└── audit/            # Security audit logging
```

## 📚 Documentation

### Assessment Documentation
- [Requirements Completion Checklist](docs/assessment/REQUIREMENTS_COMPLETION_CHECKLIST.md)
- [Security Audit Verification Report](docs/assessment/SECURITY_AUDIT_VERIFICATION.md)
- [Screenshots Evidence Guide](docs/assessment/SCREENSHOTS_EVIDENCE.md)

### Setup & Configuration
- [Secure Setup Guide](docs/setup/SECURE_SETUP_GUIDE.md)
- [Email Configuration](docs/setup/EMAIL_SETUP_GUIDE.md)
- [New Credentials](docs/setup/NEW_CREDENTIALS.md)

### Technical Documentation
- [Complete System Documentation](docs/technical/HOSPITAL_MANAGEMENT_COMPLETE_DOCUMENTATION.md)

## 📊 API Documentation

### Authentication
```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "doctor",
  "password": "1234"
}
```

### Patient Visits (Requires JWT)
```http
POST /api/visits/create
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "patientId": 2,
  "diagnosis": "Routine checkup - all vitals normal"
}
```

## 🛡️ Security Compliance

### OWASP Top 10 Coverage
1. **Broken Access Control** ✅ Role-based access with Spring Security
2. **Cryptographic Failures** ✅ AES-256 for sensitive data
3. **Injection** ✅ Parameterized queries, input validation
4. **Insecure Design** ✅ Security by design principles
5. **Security Misconfiguration** ✅ Secure defaults, no debug in production
6. **Vulnerable Components** ✅ Updated dependencies
7. **Authentication Failures** ✅ Strong password policy, account lockout
8. **Data Integrity** ✅ JWT signatures, audit logging
9. **Security Logging** ✅ Comprehensive audit trail
10. **SSRF** ✅ Input validation on all endpoints

## 📈 Performance

- Processing time: < 2 seconds per request
- Supports concurrent users with session management
- Stateless API for horizontal scaling

## 🔍 Monitoring & Logging

All security events are logged:
- Authentication attempts (success/failure)
- Access to medical data
- Data modifications
- Security violations

## 🚦 Future Enhancements

- Email notifications for security events
- Two-factor authentication
- API rate limiting
- Advanced threat detection
- Automated security scanning

## 📝 License

This project is developed for academic purposes as part of DEV6003 at New York College.

## 👥 Contributors

- Student: Alexandros Liakopoulos
- Module: DEV6003 Secure Application Development
- Institution: New York College / University of Greater Manchester
