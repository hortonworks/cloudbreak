-- // CLOUD-67915 remove recipe timeout
-- Migration SQL that makes the change goes here.

ALTER TABLE recipe DROP COLUMN IF EXISTS timeout;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE recipe ADD COLUMN timeout INTEGER;
UPDATE recipe SET timeout=15;