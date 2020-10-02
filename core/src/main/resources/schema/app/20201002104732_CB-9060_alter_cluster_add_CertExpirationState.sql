-- // CB-9060 alter cluster add CertExpirationState
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS certexpirationstate VARCHAR(255);

ALTER TABLE cluster ALTER COLUMN certexpirationstate SET DEFAULT 'VALID';

UPDATE cluster SET certexpirationstate = 'VALID' WHERE certexpirationstate IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS certexpirationstate;
