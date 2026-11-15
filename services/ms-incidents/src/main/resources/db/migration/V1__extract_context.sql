CREATE TABLE incidents (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 user_id BIGINT, type VARCHAR(255), severity VARCHAR(255), address VARCHAR(255), description VARCHAR(255),
 status VARCHAR(255), latitude DOUBLE, longitude DOUBLE, time_reported VARCHAR(255)
);
CREATE INDEX idx_incident_filter ON incidents(status,type,severity);
CREATE INDEX idx_incident_geo ON incidents(latitude,longitude);
