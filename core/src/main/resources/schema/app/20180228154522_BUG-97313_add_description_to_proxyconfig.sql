-- // BUG-97313 add description to proxyconfig
-- Migration SQL that makes the change goes here.

ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS description TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE proxyconfig DROP COLUMN IF EXISTS description;
