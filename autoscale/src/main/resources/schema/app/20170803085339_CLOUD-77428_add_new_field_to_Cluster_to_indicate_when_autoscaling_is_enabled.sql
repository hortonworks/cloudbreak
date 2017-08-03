-- // CLOUD-77428 add new field to Cluster to indicate when autoscaling is enabled
-- Migration SQL that makes the change goes here.
ALTER TABLE ONLY cluster ADD COLUMN autoscaling_enabled BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE cluster SET autoscaling_enabled=TRUE WHERE state='RUNNING';
UPDATE cluster SET autoscaling_enabled=FALSE WHERE state='DISABLED';
UPDATE cluster SET state='RUNNING' WHERE state='DISABLED';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE cluster SET state='DISABLED' WHERE autoscaling_enabled IS FALSE AND state='RUNNING';
ALTER TABLE ONLY cluster DROP COLUMN autoscaling_enabled;
