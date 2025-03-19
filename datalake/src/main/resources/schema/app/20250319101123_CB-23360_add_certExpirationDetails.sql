-- // CB-23360 alter sdxcluster add certexpirationdetails
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS certexpirationdetails VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS certexpirationdetails;
