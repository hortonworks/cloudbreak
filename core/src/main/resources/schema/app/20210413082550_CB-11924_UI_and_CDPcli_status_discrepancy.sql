-- // CB-11924 UI and CDPcli status discrepancy
-- Migration SQL that makes the change goes here.
UPDATE stackstatus
SET detailedstackstatus = 'SALT_UPDATE_FINISHED'
WHERE detailedstackstatus = 'AVAILABLE'
AND statusreason = 'Salt update finished.';

UPDATE stackstatus
SET status = 'UPDATE_IN_PROGRESS'
WHERE status = 'AVAILABLE'
AND statusreason = 'Salt update finished.';


-- //@UNDO
-- SQL to undo the change goes here.
UPDATE stackstatus
SET detailedstackstatus = 'AVAILABLE'
WHERE detailedstackstatus = 'SALT_UPDATE_FINISHED'
AND statusreason = 'Salt update finished.';

UPDATE stackstatus
SET status = 'AVAILABLE'
WHERE status = 'UPDATE_IN_PROGRESS'
AND statusreason = 'Salt update finished.';

