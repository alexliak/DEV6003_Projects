-- Create audit_log table if not exists
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    target_user VARCHAR(255),
    success BOOLEAN DEFAULT TRUE,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
);

-- Insert sample audit logs only if table is empty
INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp)
SELECT 'admin', 'AUTHENTICATION', 'User logged in successfully', true, '127.0.0.1', NOW()
WHERE NOT EXISTS (SELECT 1 FROM audit_log LIMIT 1);

INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp)
SELECT 'john.doe', 'AUTHENTICATION', 'Failed login attempt - invalid password', false, '192.168.1.100', DATE_SUB(NOW(), INTERVAL 1 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE username = 'john.doe');

INSERT INTO audit_log (username, event_type, event_description, entity_type, entity_id, success, ip_address, timestamp)
SELECT 'dr.smith', 'DATA_ACCESS', 'Accessed patient record #123', 'Patient', 123, true, '192.168.1.101', DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE username = 'dr.smith');

INSERT INTO audit_log (username, event_type, event_description, target_user, success, ip_address, timestamp)
SELECT 'admin', 'ADMIN_ACTION', 'Created new user account: nurse.jones', 'nurse.jones', true, '127.0.0.1', DATE_SUB(NOW(), INTERVAL 3 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE event_description LIKE '%nurse.jones%');

INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp)
SELECT 'dr.smith', 'PASSWORD_CHANGE', 'Password changed successfully', true, '192.168.1.101', DATE_SUB(NOW(), INTERVAL 4 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE username = 'dr.smith' AND event_type = 'PASSWORD_CHANGE');

INSERT INTO audit_log (username, event_type, event_description, entity_type, success, ip_address, timestamp)
SELECT 'secretary1', 'DATA_ACCESS', 'Created new patient record', 'Patient', true, '192.168.1.102', DATE_SUB(NOW(), INTERVAL 5 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE username = 'secretary1');

INSERT INTO audit_log (username, event_type, event_description, success, ip_address, timestamp)
SELECT 'admin', 'SECURITY_VIOLATION', 'Attempted to access restricted resource', false, '192.168.1.103', DATE_SUB(NOW(), INTERVAL 6 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE event_type = 'SECURITY_VIOLATION');
