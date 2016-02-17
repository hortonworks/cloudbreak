-- // CLOUD-49847 Show only one type of platform
-- Migration SQL that makes the change goes here.

ALTER TABLE account_preferences ADD platforms TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE account_preferences DROP COLUMN platforms;

