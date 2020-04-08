-- // CB-6513 Change monitoring user/pwd DB type from varchar(255) to TEXT
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ALTER COLUMN cloudbreakClusterManagerMonitoringUser TYPE text;
ALTER TABLE cluster ALTER COLUMN cloudbreakClusterManagerMonitoringPassword TYPE text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster ALTER COLUMN cloudbreakClusterManagerMonitoringUser TYPE varchar(255);
ALTER TABLE cluster ALTER COLUMN cloudbreakClusterManagerMonitoringPassword TYPE varchar(255);