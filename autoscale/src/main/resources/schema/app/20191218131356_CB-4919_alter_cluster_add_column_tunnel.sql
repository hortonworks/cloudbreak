-- // CB-4919 alter cluster add column tunnel
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS tunnel VARCHAR(255);
UPDATE cluster SET tunnel='DIRECT' WHERE tunnel is null;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS tunnel;
