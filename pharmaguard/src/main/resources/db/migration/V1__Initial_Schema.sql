-- =========================================================================
-- PHARMAGUARD DATABASE MIGRATION V1: INITIAL PRODUCTION SCHEMA
-- =========================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STAFF',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- 2. MEDICINES TABLE
CREATE TABLE IF NOT EXISTS medicines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_name VARCHAR(100) NOT NULL,
    generic_name VARCHAR(100),
    category VARCHAR(50) NOT NULL,
    batch_number VARCHAR(50) NOT NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 0,
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    expiry_date DATE NOT NULL,
    reorder_level INT NOT NULL DEFAULT 10,
    supplier_name VARCHAR(100),
    lead_time_days INT NOT NULL DEFAULT 7,
    safety_stock INT NOT NULL DEFAULT 10,
    storage_location VARCHAR(100) DEFAULT 'Main Rack',
    strength VARCHAR(50),
    dosage_form VARCHAR(50),
    manufacturer VARCHAR(100),
    barcode VARCHAR(100),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_med_name ON medicines(medicine_name);
CREATE INDEX idx_med_batch ON medicines(batch_number);
CREATE INDEX idx_med_expiry ON medicines(expiry_date);
CREATE INDEX idx_med_category ON medicines(category);
CREATE INDEX idx_med_barcode ON medicines(barcode);
CREATE INDEX idx_med_quantity ON medicines(quantity);

-- 3. MEDICINE BATCHES TABLE
CREATE TABLE IF NOT EXISTS medicine_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_number VARCHAR(50) NOT NULL UNIQUE,
    medicine_id BIGINT,
    manufacture_date DATE,
    expiry_date DATE NOT NULL,
    initial_quantity INT NOT NULL DEFAULT 0,
    current_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE'
);

CREATE INDEX idx_batch_number ON medicine_batches(batch_number);

-- 4. SUPPLIERS TABLE
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL UNIQUE,
    contact_person VARCHAR(100),
    email VARCHAR(150),
    phone VARCHAR(50),
    address VARCHAR(255)
);

CREATE INDEX idx_supplier_name ON suppliers(supplier_name);

-- 5. STOCK TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS stock_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    transaction_date DATETIME NOT NULL,
    notes VARCHAR(255),
    previous_stock INT,
    new_stock INT,
    performed_by VARCHAR(100),
    CONSTRAINT fk_st_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

CREATE INDEX idx_st_medicine_id ON stock_transactions(medicine_id);
CREATE INDEX idx_st_date ON stock_transactions(transaction_date);
CREATE INDEX idx_st_type ON stock_transactions(transaction_type);

-- 6. ALERTS TABLE
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(200),
    message TEXT NOT NULL,
    medicine_id BIGINT,
    medicine_name VARCHAR(100),
    verification_id BIGINT,
    severity VARCHAR(50) DEFAULT 'MEDIUM',
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    resolved_at DATETIME
);

CREATE INDEX idx_alert_med_id ON alerts(medicine_id);
CREATE INDEX idx_alert_type ON alerts(alert_type);
CREATE INDEX idx_alert_resolved ON alerts(resolved);

-- 7. EXPIRY NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS expiry_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    days_remaining BIGINT NOT NULL,
    alert_key VARCHAR(150) NOT NULL UNIQUE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME
);

CREATE INDEX idx_exp_user_id ON expiry_notifications(user_id);
CREATE INDEX idx_exp_med_id ON expiry_notifications(medicine_id);
CREATE INDEX idx_exp_is_read ON expiry_notifications(is_read);

-- 8. VERIFICATION HISTORIES TABLE
CREATE TABLE IF NOT EXISTS verification_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    barcode_hash VARCHAR(100),
    generic_name VARCHAR(100),
    strength VARCHAR(50),
    dosage_form VARCHAR(50),
    raw_ocr_text TEXT,
    extracted_info_json TEXT,
    verification_signals_json TEXT,
    detected_issues_json TEXT,
    risk_score INT NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    verification_status VARCHAR(100) NOT NULL,
    ai_explanation TEXT,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_vh_user_id ON verification_histories(user_id);
CREATE INDEX idx_vh_batch ON verification_histories(batch_number);
CREATE INDEX idx_vh_risk ON verification_histories(risk_level);
CREATE INDEX idx_vh_barcode ON verification_histories(barcode_hash);

-- 9. SCAN EVENTS TABLE
CREATE TABLE IF NOT EXISTS scan_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    verification_id BIGINT,
    user_id BIGINT,
    barcode_hash VARCHAR(100) NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    device_session_id VARCHAR(100),
    location_region VARCHAR(100),
    verification_status VARCHAR(100),
    scan_timestamp DATETIME NOT NULL
);

CREATE INDEX idx_scan_barcode ON scan_events(barcode_hash);
CREATE INDEX idx_scan_batch ON scan_events(batch_number);
CREATE INDEX idx_scan_timestamp ON scan_events(scan_timestamp);

-- 10. AUDIT LOGS TABLE
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    timestamp DATETIME NOT NULL,
    ip_address VARCHAR(50),
    result VARCHAR(50),
    request_id VARCHAR(100)
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);

-- 11. PHARMACY SETTINGS TABLE
CREATE TABLE IF NOT EXISTS pharmacy_settings (
    id BIGINT PRIMARY KEY,
    pharmacy_name VARCHAR(150),
    license_number VARCHAR(100),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(50),
    address VARCHAR(255),
    low_stock_threshold INT DEFAULT 10,
    expiry_warning_days INT DEFAULT 30,
    enable_email_alerts BOOLEAN DEFAULT TRUE,
    enable_ai_verification BOOLEAN DEFAULT TRUE
);
