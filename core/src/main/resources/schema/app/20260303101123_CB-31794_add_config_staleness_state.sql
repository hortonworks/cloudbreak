-- // CB-31794 alter cluster add config staleness state
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS configstalenessstate VARCHAR(255);

ALTER TABLE cluster ALTER COLUMN configstalenessstate SET DEFAULT 'FRESH';

UPDATE cluster SET configstalenessstate = 'FRESH' WHERE configstalenessstate IS NULL;

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS configstalenessdetails TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS configstalenessstate;
ALTER TABLE cluster DROP COLUMN IF EXISTS configstalenessdetails;
