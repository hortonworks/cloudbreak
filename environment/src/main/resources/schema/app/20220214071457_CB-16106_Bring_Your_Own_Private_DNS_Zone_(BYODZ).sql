-- // CB-16106 Bring Your Own Private DNS Zone (BYODZ)
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS privatednszoneid VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS privatednszoneid;


