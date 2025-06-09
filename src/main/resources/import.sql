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
INSERT INTO hospuser (id, username, password, email, first_name, last_name, date_of_birth, account_locked, failed_login_attempts, last_password_change, lastlogondatetime) VALUES 
    (1, 'admin', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'admin@hospital.nyc', 'System', 'Administrator', '1980-01-01', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:33:17.263707'),
    (2, 'alex', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'alex.liakopoulos@gmail.com', 'Alexandros', 'Liakopoulos', '1995-07-18', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:33:40.087770'),
    (3, 'george', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'george@hospital.nyc', 'George', 'Papadopoulos', '1975-05-15', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:34:11.024450'),
    (4, 'secretary', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'secretary@hospital.nyc', 'Anna', 'Dimitriou', '1990-03-10', false, 0, CURRENT_TIMESTAMP, '2025-03-14 10:34:11.024450'),
    (5, 'maria', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'maria@hospital.nyc', 'Maria', 'Nikolaou', '1982-08-22', false, 0, CURRENT_TIMESTAMP, NULL),
    (6, 'nora', '$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa', 'nora@hospital.nyc', 'Nora', 'Constantinou', '1988-11-30', false, 0, CURRENT_TIMESTAMP, NULL),
    (7, 'doctor2', '$2a$12$RZLhKYKMH5JBk5DPRoZrB.Gk5pVgYa1Kc8pq/FHxr3CYQvKJtXVhO', 'doctor2@hospital.nyc', 'Dimitris', 'Karagiannis', '1978-12-05', false, 0, CURRENT_TIMESTAMP, NULL),
    (8, 'patient1', '$2a$12$RZLhKYKMH5JBk5DPRoZrB.Gk5pVgYa1Kc8pq/FHxr3CYQvKJtXVhO', 'patient1@hospital.nyc', 'Kostas', 'Stavrou', '1992-04-20', false, 0, CURRENT_TIMESTAMP, NULL);

-- Link users to roles (updated mapping)
INSERT INTO user_roles (user_id, role_id) VALUES 
    (1, 1), -- admin has ROLE_ADMIN
    (2, 3), -- alex has ROLE_PATIENT  
    (3, 2), -- george has ROLE_DOCTOR
    (4, 4), -- secretary has ROLE_SECRETARIAT
    (5, 2), -- maria has ROLE_DOCTOR
    (6, 3), -- nora has ROLE_PATIENT
    (7, 2), -- doctor2 has ROLE_DOCTOR
    (8, 3); -- patient1 has ROLE_PATIENT

-- Additional secure users for testing (password: Password123!)
-- These demonstrate the new password policy
-- Password "Password123!" encoded: $2a$12$RZLhKYKMH5JBk5DPRoZrB.Gk5pVgYa1Kc8pq/FHxr3CYQvKJtXVhO

-- Sample patient visits (with encrypted diagnosis for security)
-- Note: These are sample encrypted values - real encryption happens at runtime
INSERT INTO patientvisit (visitdate, encrypted_diagnosis, patient_id, doctor_id) VALUES
    ('2025-01-15', 'dGhpcyBpcyBhIHNhbXBsZSBlbmNyeXB0ZWQgZGlhZ25vc2lz', 2, 3), -- alex visited george
    ('2025-01-20', 'YW5vdGhlciBlbmNyeXB0ZWQgZGlhZ25vc2lzIGV4YW1wbGU=', 2, 5), -- alex visited maria
    ('2025-01-22', 'ZW5jcnlwdGVkIGRpYWdub3NpcyBmb3IgcGF0aWVudA==', 6, 3), -- nora visited george
    ('2025-01-25', 'c2FtcGxlIGVuY3J5cHRlZCBtZWRpY2FsIGRhdGE=', 8, 7); -- patient1 visited doctor2

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
