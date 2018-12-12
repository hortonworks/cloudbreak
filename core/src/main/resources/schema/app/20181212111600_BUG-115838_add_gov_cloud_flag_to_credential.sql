-- // BUG-115838 add gov cloud flag to credential
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD COLUMN govCloud boolean DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE credential DROP COLUMN govCloud;