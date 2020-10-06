-- // CB-9060 alter cluster add CertExpirationState
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS certexpirationstate VARCHAR(255);

ALTER TABLE sdxcluster ALTER COLUMN certexpirationstate SET DEFAULT 'VALID';

UPDATE sdxcluster SET certexpirationstate = 'VALID' WHERE certexpirationstate IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS certexpirationstate;
