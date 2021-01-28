-- // CB-10656: Limit blueprint upgrade
-- Migration SQL that makes the change goes here.
ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS blueprintupgradeoption varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE blueprint DROP COLUMN IF EXISTS blueprintupgradeoption;


