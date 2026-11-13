-- // CB-35498 Disable notification by default for every Data Lake
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ALTER COLUMN notificationstate SET DEFAULT 'DISABLED';
UPDATE sdxcluster SET notificationstate = 'DISABLED' WHERE notificationstate IS DISTINCT FROM 'DISABLED';

-- //@UNDO
-- SQL to undo the change goes here.
-- The previous per-cluster values are not recoverable, so only the column default is restored.

ALTER TABLE sdxcluster ALTER COLUMN notificationstate SET DEFAULT 'ENABLED';
