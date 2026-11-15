CREATE TABLE incident_evidences (
 id VARCHAR(36) PRIMARY KEY, incident_id BIGINT NOT NULL,
 original_name VARCHAR(180) NOT NULL, content_type VARCHAR(100) NOT NULL,
 byte_size BIGINT NOT NULL, state VARCHAR(20) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (incident_id) REFERENCES incidents(id),
 CONSTRAINT ck_evidence_state CHECK (state IN ('PENDING','AVAILABLE','FAILED'))
);
