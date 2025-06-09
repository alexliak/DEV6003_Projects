-- Create audit_log table for security logging
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

-- Insert some sample audit logs
INSERT INTO audit_log (username, event_type, event_description, success, ip_address) VALUES
('admin', 'AUTHENTICATION', 'User logged in successfully', true, '127.0.0.1'),
('john.doe', 'AUTHENTICATION', 'Failed login attempt - invalid password', false, '192.168.1.100'),
('dr.smith', 'DATA_ACCESS', 'Accessed patient record #123', true, '192.168.1.101'),
('admin', 'ADMIN_ACTION', 'Created new user account: nurse.jones', true, '127.0.0.1'),
('dr.smith', 'PASSWORD_CHANGE', 'Password changed successfully', true, '192.168.1.101'),
('secretary1', 'DATA_ACCESS', 'Created new patient record', true, '192.168.1.102'),
('admin', 'SECURITY_VIOLATION', 'Attempted to access restricted resource', false, '192.168.1.103');
