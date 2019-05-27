-- // CB-1555 new entries in flowlog tables: flowTriggerUserCrn
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog add COLUMN flowtriggerusercrn varchar(255) NULL;
ALTER TABLE IF EXISTS flowchainlog add COLUMN flowtriggerusercrn varchar(255) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS flowlog DROP COLUMN flowtriggerusercrn;
ALTER TABLE IF EXISTS flowchainlog DROP COLUMN flowtriggerusercrn;
