# SQL Verification Results - DEV6003 Assessment 002

## Database: nycsecdb
**Date of Verification:** June 9, 2025

---

## 1. USERS AND ROLES VERIFICATION

### Query:
```sql
SELECT 
    h.id,
    h.username,
    h.email,
    CONCAT(h.first_name, ' ', h.last_name) as full_name,
    r.name as role,
    h.account_locked,
    h.failed_login_attempts
FROM hospuser h 
JOIN user_roles ur ON h.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id
ORDER BY r.name, h.username;
```

### Results:
```
+----+-----------+----------------------+-------------------------------------------+------------------+----------------+-----------------------+
| id | username  | email                | full_name                                 | role             | account_locked | failed_login_attempts |
+----+-----------+----------------------+-------------------------------------------+------------------+----------------+-----------------------+
|  1 | admin     | admin@admin.com      | Γιάννης Διαχειριστής                      | ROLE_ADMIN       | 0x00          |                     0 |
|  5 | admin2    | admin2@hospital.com  | Μαρία Αδμίνη                              | ROLE_ADMIN       | 0x00          |                     0 |
|  3 | doctor    | doc@doc.com          | Δημήτρης Παπαδόπουλος                     | ROLE_DOCTOR      | 0x00          |                     0 |
|  6 | doctor2   | doctor2@hospital.com | Ελένη Ιατρού                              | ROLE_DOCTOR      | 0x00          |                     0 |
| 11 | alex      | alex@mail.com        | Κώστας Αλεξίου                            | ROLE_PATIENT     | 0x00          |                     0 |
| 13 | Jdoe      | Jdoe@mail.com        | John Doe                                  | ROLE_PATIENT     | 0x00          |                     0 |
|  2 | patient   | pat@pat.com          | Νίκος Ασθενής                             | ROLE_PATIENT     | 0x00          |                     0 |
| 10 | testuser  | test@test.com        | Σοφία Τεστάκη                             | ROLE_PATIENT     | 0x00          |                     0 |
| 12 | user1     | user1@mail.com       | Πέτρος Χρήστου                            | ROLE_PATIENT     | 0x00          |                     0 |
|  4 | secretary | secr@doc.com         | Κατερίνα Γραμματέα                        | ROLE_SECRETARIAT | 0x00          |                     0 |
+----+-----------+----------------------+-------------------------------------------+------------------+----------------+-----------------------+
```

**✅ Verification:** All 4 roles implemented correctly (ADMIN, DOCTOR, PATIENT, SECRETARIAT)

---

## 2. PASSWORD SECURITY STATUS

### Query:
```sql
SELECT 
    username,
    last_password_change,
    DATEDIFF(NOW(), last_password_change) as days_since_change,
    CASE
        WHEN DATEDIFF(NOW(), last_password_change) > 90 THEN 'EXPIRED'
        WHEN DATEDIFF(NOW(), last_password_change) > 80 THEN 'EXPIRING SOON'
        ELSE 'ACTIVE'
    END as password_status,
    force_password_change
FROM hospuser;
```

### Results:
```
+-----------+----------------------------+-------------------+-----------------+-----------------------+
| username  | last_password_change       | days_since_change | password_status | force_password_change |
+-----------+----------------------------+-------------------+-----------------+-----------------------+
| admin     | 2025-06-07 16:48:30.657000 |                 2 | ACTIVE          | 0x00                 |
| patient   | 2025-06-07 04:24:46.000000 |                 2 | ACTIVE          | 0x00                 |
| doctor    | 2025-06-07 04:24:46.000000 |                 2 | ACTIVE          | 0x00                 |
| secretary | 2025-06-09 15:48:30.523000 |                 0 | ACTIVE          | 0x00                 |
| admin2    | 2025-06-07 04:24:46.000000 |                 2 | ACTIVE          | 0x00                 |
| doctor2   | 2025-06-07 22:07:59.554000 |                 2 | ACTIVE          | 0x00                 |
| testuser  | 2025-06-07 21:53:05.209000 |                 2 | ACTIVE          | 0x00                 |
| alex      | 2025-06-07 21:22:25.889000 |                 2 | ACTIVE          | 0x00                 |
| user1     | 2025-06-08 13:36:32.943000 |                 1 | ACTIVE          | 0x00                 |
| Jdoe      | 2025-06-09 18:15:25.600000 |                 0 | ACTIVE          | 0x00                 |
+-----------+----------------------------+-------------------+-----------------+-----------------------+
```

**✅ Verification:** 
- All users have recent password changes
- 90-day expiry policy implemented
- Force password change functionality available

---

## 3. ENCRYPTION VERIFICATION

### Query:
```sql
SELECT 
    COUNT(*) as total_visits,
    COUNT(encrypted_diagnosis) as encrypted_visits,
    COUNT(diagnosis) as plain_text_visits
FROM patientvisit;
```

### Results:
```
+--------------+------------------+-------------------+
| total_visits | encrypted_visits | plain_text_visits |
+--------------+------------------+-------------------+
|           15 |               15 |                 0 |
+--------------+------------------+-------------------+
```

**✅ Verification:** 100% of diagnoses are encrypted (15/15). NO plain text diagnoses exist.

---

## 4. AUDIT LOG SUMMARY

### Query:
```sql
SELECT
    event_type,
    COUNT(*) as count,
    COUNT(CASE WHEN success = 1 THEN 1 END) as successful,
    COUNT(CASE WHEN success = 0 THEN 1 END) as failed
FROM audit_log
GROUP BY event_type;
```

### Results:
```
+--------------------+-------+------------+--------+
| event_type         | count | successful | failed |
+--------------------+-------+------------+--------+
| AUTHENTICATION     |    23 |         19 |      4 |
| DATA_ACCESS        |    10 |         10 |      0 |
| SECURITY_VIOLATION |     9 |          0 |      9 |
+--------------------+-------+------------+--------+
```

**✅ Verification:** 
- Authentication tracking: 19 successful, 4 failed logins
- Data access logging: All 10 accesses successful
- Security violations: 9 attempts blocked

---

## 5. SECURITY VIOLATIONS DETAIL

### Query:
```sql
SELECT * FROM audit_log 
WHERE event_type = 'SECURITY_VIOLATION' 
ORDER BY timestamp DESC;
```

### Results:
```
+----+----------+--------------------+--------------------------------------------------------------+---------------------+
| id | username | event_type         | event_description                                            | timestamp           |
+----+----------+--------------------+--------------------------------------------------------------+---------------------+
| 42 | doctor2  | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-09 18:55:18 |
| 41 | doctor2  | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-09 18:51:14 |
| 40 | doctor2  | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-09 18:32:47 |
| 38 | doctor   | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-07 11:43:39 |
| 36 | doctor   | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-07 11:42:06 |
| 33 | doctor   | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-07 10:51:02 |
| 34 | doctor   | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-07 10:51:02 |
| 27 | doctor   | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-07 10:46:42 |
| 28 | doctor   | SECURITY_VIOLATION | MALICIOUS_INPUT: Attempted SQL injection or XSS in diagnosis | 2025-06-07 10:46:42 |
+----+----------+--------------------+--------------------------------------------------------------+---------------------+
```

**✅ Verification:** All SQL injection and XSS attempts were detected and blocked

---

## SUMMARY OF SECURITY VERIFICATION

| Security Feature | Status | Evidence |
|------------------|--------|----------|
| **Input Validation** | ✅ WORKING | 9 malicious attempts blocked |
| **Password Policy** | ✅ ACTIVE | All users with recent changes |
| **Role System** | ✅ COMPLETE | 4 roles with 10 users |
| **Encryption** | ✅ 100% | All 15 diagnoses encrypted |
| **Audit Logging** | ✅ OPERATIONAL | 42 total events logged |
| **Account Security** | ✅ FUNCTIONAL | No locked accounts, failed attempts tracked |

---

## Additional Verification Queries

### Check Password History:
```sql
SELECT username, 
       CASE 
           WHEN password_history IS NOT NULL THEN 'Has History' 
           ELSE 'No History' 
       END as history_status,
       (LENGTH(password_history) - LENGTH(REPLACE(password_history, '$2a$', ''))) / 4 as passwords_in_history
FROM hospuser 
WHERE password_history IS NOT NULL;
```

### Verify No Plain Text Passwords:
```sql
SELECT COUNT(*) as insecure_passwords 
FROM hospuser 
WHERE password NOT LIKE '$2a$%' 
   OR password IS NULL;
-- Result should be 0
```

### Check Latest Security Events:
```sql
SELECT 
    DATE_FORMAT(timestamp, '%Y-%m-%d %H:%i') as event_time,
    username,
    event_type,
    success,
    ip_address
FROM audit_log 
ORDER BY timestamp DESC 
LIMIT 20;
```

---

**Document Generated:** June 9, 2025  
**Assessment:** DEV6003 Secure Application Development  
**Status:** READY FOR SUBMISSION
