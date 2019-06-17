-- // CB-1570 eliminating environment from core
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS credentialcrn;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS credentialcrn varchar(255);
