-- // CB-10977 Clarify on reason Force Deletion is allowed on environments
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS deletionType varchar(255);
UPDATE environment SET deletionType='NONE' where archived IS FALSE;
UPDATE environment SET deletionType='SIMPLE' where archived IS TRUE;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS deletionType;
