-- // CB-4502 consolidate tagging
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN tags text;
UPDATE environment SET tags='{"userDefinedTags":{},"defaultTags":{}}';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS tags;