-- Original import.sql από τον καθηγητή με security enhancements
-- Οι original χρήστες έχουν password: 1234

-- Create roles with new enum structure
INSERT INTO roles (id, name, description) VALUES 
    (1, 'ROLE_ADMIN', 'System Administrator with full access'),
    (2, 'ROLE_DOCTOR', 'Doctor with patient access and visit management'),
    (3, 'ROLE_PATIENT', 'Patient with limited access to own data'),
    (4, 'ROLE_SECRETARIAT', 'Secretary with user management access');

-- Insert original users with BCrypt encoded password "1234"
-- Password "1234" encoded with BCrypt strength 12: $2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa
INSERT INTO hospuser (id, username, password, email, account_locked, failed_login_attempts, last_password_change, lastlogondatetime) VALUES 
    (1, 'admin', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'admin@admin.com', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:33:17.263707'),
    (2, 'patient', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'pat@pat.com', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:33:40.087770'),
    (3, 'doctor', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'doc@doc.com', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:34:11.024450'),
    (4, 'secretary', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'secr@doc.com', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:34:11.024450');

-- Link users to roles (original mapping)
INSERT INTO user_roles (user_id, role_id) VALUES 
    (1, 1), -- admin has ROLE_ADMIN
    (2, 3), -- patient has ROLE_PATIENT
    (3, 2), -- doctor has ROLE_DOCTOR
    (4, 4); -- secretary has ROLE_SECRETARIAT

-- Additional secure users for testing (password: Password123!)
-- These demonstrate the new password policy
INSERT INTO hospuser (id, username, password, email, account_locked, failed_login_attempts, last_password_change) VALUES 
    (5, 'admin2', '$2a$12$RZLhKYKMH5JBk5DPRoZrB.Gk5pVgYa1Kc8pq/FHxr3CYQvKJtXVhO', 'admin2@hospital.com', false, 0, CURRENT_TIMESTAMP),
    (6, 'doctor2', '$2a$12$RZLhKYKMH5JBk5DPRoZrB.Gk5pVgYa1Kc8pq/FHxr3CYQvKJtXVhO', 'doctor2@hospital.com', false, 0, CURRENT_TIMESTAMP);

INSERT INTO user_roles (user_id, role_id) VALUES 
    (5, 1), -- admin2 has ROLE_ADMIN
    (6, 2); -- doctor2 has ROLE_DOCTOR

-- Sample patient visits (with encrypted diagnosis for security)
-- Note: These are sample encrypted values - real encryption happens at runtime
INSERT INTO patientvisit (visitdate, encrypted_diagnosis, patient_id, doctor_id) VALUES
    ('2025-01-15', 'dGhpcyBpcyBhIHNhbXBsZSBlbmNyeXB0ZWQgZGlhZ25vc2lz', 2, 3),
    ('2025-01-20', 'YW5vdGhlciBlbmNyeXB0ZWQgZGlhZ25vc2lzIGV4YW1wbGU=', 2, 3);

-- Create audit log table for security events
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100),
    event_type VARCHAR(50),
    event_description VARCHAR(500),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    target_user VARCHAR(100),
    success BOOLEAN,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type)
);
