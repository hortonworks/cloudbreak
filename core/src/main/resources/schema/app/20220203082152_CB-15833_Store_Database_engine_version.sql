-- // CB-15833 Store Database engine version
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS externaldatabaseengineversion VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS externaldatabaseengineversion;
