-- // CB-29301 Freeipa architecture on environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeipaarchitecture varchar(255);
UPDATE environment SET freeipaarchitecture = 'X86_64' WHERE freeipaarchitecture IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.