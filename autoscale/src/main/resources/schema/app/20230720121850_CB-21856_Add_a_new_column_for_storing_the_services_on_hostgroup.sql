-- // CB-21856 Add a new column for storing the services on hostgroup
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS blueprinttext TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS blueprinttext;