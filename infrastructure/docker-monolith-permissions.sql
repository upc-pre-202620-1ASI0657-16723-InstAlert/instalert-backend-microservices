-- Fresh Docker volume only. Runtime monolith cannot create/drop schema objects.
REVOKE ALL PRIVILEGES ON instalert_db.* FROM 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.incidents TO 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.communities TO 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.notification_contacts TO 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.community_notification_preferences TO 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.community_processed_alert_events TO 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.community_alert_recipients TO 'monolith_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON instalert_db.community_alert_inbox TO 'monolith_app'@'%';
