-- // CB-29084 Add column 'providersyncstates' column to 'stack'
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS providersyncstates TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS providersyncstates;
