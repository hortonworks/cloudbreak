-- // CB-437 mpack ignorevalidation flag added
-- Migration SQL that makes the change goes here.

ALTER TABLE managementpack ADD COLUMN IF NOT EXISTS ignoreValidation boolean DEFAULT FALSE;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE managementpack DROP COLUMN IF EXISTS ignoreValidation;
