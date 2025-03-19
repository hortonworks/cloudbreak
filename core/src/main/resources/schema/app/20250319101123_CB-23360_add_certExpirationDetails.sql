-- // CB-23360 alter cluster add certexpirationdetails
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS certexpirationdetails VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS certexpirationdetails;
