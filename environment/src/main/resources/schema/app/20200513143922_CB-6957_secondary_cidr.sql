-- // CB-6957 secondary cidr
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS networkCidrs text;
UPDATE environment_network SET networkCidrs=networkCidr;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS networkCidrs;


