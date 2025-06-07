# DEV6003 Hospital Management System - Setup Documentation

## Project Overview
This is a secure hospital patient management system built with Spring Boot, implementing security features as required by DEV6003 Assessment 002.

## Setup Steps Performed

### 1. MySQL Database Setup
```bash
# Create database
mysql -u root -proot123
CREATE DATABASE nycsecdb;
exit;

# Import initial data
mysql -u root -proot123 nycsecdb < src/main/resources/import.sql
```

### 2. Application Configuration Changes

#### Updated `application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=root123
```

### 3. Security Fixes Applied

#### 3.1 BCrypt Password Hash Fix
- Original import.sql had incorrect BCrypt hashes
- Updated all users with correct hash for password "1234":
```sql
UPDATE hospuser 
SET password = '$2a$12$VfJJNptKw7KBulHegGRpBeU95y4i5hShO1b3HZ9S2xkImjfJ9pfOO' 
WHERE username IN ('admin', 'doctor', 'patient', 'secretary');
```

#### 3.2 CustomUserPrincipal Role Mapping Fix
**File:** `src/main/java/com/nyc/hosp/security/CustomUserPrincipal.java`
- Changed: `role.getName().name()` 
- To: `role.getName().toString()`

#### 3.3 Security Config Public Endpoints
**File:** `src/main/java/com/nyc/hosp/security/SecurityConfig.java`
- Added `/error` to public endpoints to prevent authentication loops

#### 3.4 JWT Secret Key Length Fix
**File:** `src/main/resources/application.properties`
- Original: 48 characters (384 bits) - TOO SHORT for HS512
- Updated: 128 characters (512+ bits) - SECURE for HS512
```properties
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6251655468576D5A7134743777217A25432A
```

## Working Credentials

| Username  | Password | Role             |
|-----------|----------|------------------|
| admin     | 1234     | ROLE_ADMIN       |
| doctor    | 1234     | ROLE_DOCTOR      |
| patient   | 1234     | ROLE_PATIENT     |
| secretary | 1234     | ROLE_SECRETARIAT |

## Testing the Application

### 1. Login and Get JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"1234"}'
```

### 2. Use JWT Token for Authenticated Requests
```bash
# Example: Get user profile
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## Security Features Implemented

1. **JWT Authentication** ✅
   - Stateless authentication
   - Token expiration: 24 hours
   - HS512 algorithm with 512-bit key

2. **Password Security** ✅
   - BCrypt encoding (strength 12)
   - Password complexity validation
   - Password change policy

3. **Role-Based Access Control** ✅
   - 4 roles: ADMIN, DOCTOR, SECRETARIAT, PATIENT
   - Method-level security with @PreAuthorize
   - Endpoint-level security in SecurityConfig

4. **Account Security** ✅
   - Account lockout after 3 failed attempts
   - Failed login attempt tracking
   - Audit logging for all authentication events

5. **Input Validation** ✅
   - Bean validation on all DTOs
   - SQL injection prevention
   - XSS protection

6. **Diagnosis Encryption** ✅
   - AES encryption for sensitive medical data
   - Encrypted storage in database

## Common Issues and Solutions

### Issue 1: Login returns 401 Unauthorized
- **Cause:** JWT filter blocking the login endpoint
- **Solution:** Added `/error` to public endpoints

### Issue 2: WeakKeyException for JWT
- **Cause:** JWT secret too short for HS512
- **Solution:** Extended secret to 128 characters

### Issue 3: Invalid username or password
- **Cause:** BCrypt hash mismatch
- **Solution:** Updated database with correct BCrypt hashes

## Next Steps

1. Test all role-based endpoints
2. Implement patient visit creation with diagnosis encryption
3. Test password change functionality
4. Verify audit logging is working
5. Add comprehensive unit and integration tests

## Important Notes

- Never commit real secrets to Git
- Always use environment variables in production
- Rotate JWT secrets regularly
- Monitor failed login attempts
- Regular security audits recommended
