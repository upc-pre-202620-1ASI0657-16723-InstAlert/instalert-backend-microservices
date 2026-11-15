CREATE TABLE community_memberships (
 community_id BIGINT NOT NULL, account_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(community_id,account_id), FOREIGN KEY(community_id) REFERENCES communities(id) ON DELETE CASCADE
);
CREATE TABLE community_support_requests (
 id VARCHAR(36) PRIMARY KEY, community_id BIGINT NOT NULL, account_id VARCHAR(36) NOT NULL,
 description VARCHAR(1000) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY(community_id) REFERENCES communities(id) ON DELETE CASCADE
);
CREATE TABLE monitoring_grants (
 id VARCHAR(36) PRIMARY KEY, owner_account_id VARCHAR(36) NOT NULL, recipient_account_id VARCHAR(36) NOT NULL,
 scope VARCHAR(30) NOT NULL, expires_at TIMESTAMP(6) NOT NULL, revoked INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_monitoring_permission ON monitoring_grants(owner_account_id,recipient_account_id,revoked,expires_at);
