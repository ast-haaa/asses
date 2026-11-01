-- =============================================================================
-- Cynbit Technologies Backend Persistence Schema (MySQL 8.0+)
-- Character Set: utf8mb4, Collation: utf8mb4_unicode_ci
-- =============================================================================

CREATE DATABASE IF NOT EXISTS cynbit_remediation_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cynbit_remediation_db;

-- 1. Users Table (RBAC Authentication)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY COMMENT 'UUID identifier',
    username VARCHAR(100) NOT NULL UNIQUE COMMENT 'Unique login identifier/email',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt hash (cost 12)',
    role VARCHAR(50) NOT NULL COMMENT 'ROLE_SUPER_ADMIN, ROLE_HR_ADMIN, ROLE_MENTOR',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role)
) ENGINE=InnoDB COMMENT='Portal administrative users with strict RBAC';

-- 2. Contact Inquiries Table
CREATE TABLE IF NOT EXISTS inquiries (
    id VARCHAR(64) PRIMARY KEY COMMENT 'Unique inquiry reference (e.g., inq_...)',
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    company VARCHAR(100),
    industry VARCHAR(100),
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_inquiries_email (email),
    INDEX idx_inquiries_created_at (created_at)
) ENGINE=InnoDB COMMENT='Prospective client inquiries received from marketing site';

-- 3. Career Applications Table
CREATE TABLE IF NOT EXISTS career_applications (
    id VARCHAR(64) PRIMARY KEY COMMENT 'Unique candidate tracking reference (e.g., app_...)',
    job_id VARCHAR(100) NOT NULL COMMENT 'Job vacancy identifier',
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    whatsapp VARCHAR(20),
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    notice_period VARCHAR(50) NOT NULL,
    skills TEXT NOT NULL,
    current_city VARCHAR(100) NOT NULL,
    permanent_address TEXT NOT NULL,
    linkedin_url VARCHAR(500) NOT NULL,
    portfolio_url VARCHAR(500),
    message TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED, UNDER_REVIEW, SHORTLISTED, REJECTED',
    agreed_to_terms BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Enforce DB-level idempotency to prevent duplicate submissions per candidate per job
    UNIQUE KEY uq_career_candidate_job (email, job_id),
    INDEX idx_career_job_id (job_id),
    INDEX idx_career_status (status),
    INDEX idx_career_created_at (created_at)
) ENGINE=InnoDB COMMENT='Candidate job applications with strict duplicate constraints';

-- 4. Career Education History Table
CREATE TABLE IF NOT EXISTS career_education_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id VARCHAR(64) NOT NULL,
    degree VARCHAR(100) NOT NULL,
    institution VARCHAR(255) NOT NULL,
    year_of_passing INT NOT NULL,
    percentage_or_cgpa VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_education_application
        FOREIGN KEY (application_id) REFERENCES career_applications(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_education_app (application_id)
) ENGINE=InnoDB COMMENT='Education background records associated with applications';

-- 5. Official Documents Table (Verification & Anti-Collision)
CREATE TABLE IF NOT EXISTS documents (
    reference_number VARCHAR(100) PRIMARY KEY COMMENT 'Formatted ref: CB/{DEPT}/{TYPE}/{YEAR}/{SEQ}-{RAND}',
    document_type VARCHAR(50) NOT NULL COMMENT 'OFFER_LETTER, INTERNSHIP_CERTIFICATE, APPOINTMENT_LETTER',
    candidate_name VARCHAR(100) NOT NULL,
    employment_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, REVOKED, EXPIRED',
    joining_date VARCHAR(50),
    salary VARCHAR(100),
    duration VARCHAR(100),
    reporting_to VARCHAR(100),
    valid_until VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_documents_type (document_type),
    INDEX idx_documents_candidate (candidate_name),
    INDEX idx_documents_created_at (created_at)
) ENGINE=InnoDB COMMENT='Tamper-resistant official credentials and letters';

-- 6. Token Blacklist Table (Distributed Logout State)
CREATE TABLE IF NOT EXISTS token_blacklist (
    token_hash VARCHAR(64) PRIMARY KEY COMMENT 'SHA-256 hash of revoked JWT token',
    expiration_epoch_ms BIGINT NOT NULL COMMENT 'Epoch timestamp when token naturally expires',
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_blacklist_exp (expiration_epoch_ms)
) ENGINE=InnoDB COMMENT='Invalidated Bearer tokens stored until natural expiry';

