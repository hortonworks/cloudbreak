-- // CB-29084 Add column 'providersyncstates' column to 'sdxcluster'
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS providersyncstates TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS providersyncstates;
