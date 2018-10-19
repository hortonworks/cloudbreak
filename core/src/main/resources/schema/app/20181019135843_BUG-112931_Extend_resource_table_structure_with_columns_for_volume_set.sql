-- // BUG-112931 Extend resource table structure with columns for volume set
-- Migration SQL that makes the change goes here.

ALTER TABLE resource ADD COLUMN IF NOT EXISTS instanceid varchar(255);
ALTER TABLE resource ADD COLUMN IF NOT EXISTS attributes TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE resource DROP COLUMN IF EXISTS instanceid;
ALTER TABLE resource DROP COLUMN IF EXISTS attributes;


