-- // CB-18305: Make necessary changes to make the DL resizing transparent to experiences and UI
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS architecture varchar(255);
UPDATE sdxcluster SET architecture = 'X86_64' WHERE architecture IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.
