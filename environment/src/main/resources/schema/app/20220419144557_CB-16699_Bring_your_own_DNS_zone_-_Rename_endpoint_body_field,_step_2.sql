-- // CB-16699 Bring your own DNS zone - Rename endpoint body field, step 2
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS privatednszoneid;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS privatednszoneid VARCHAR(255);
