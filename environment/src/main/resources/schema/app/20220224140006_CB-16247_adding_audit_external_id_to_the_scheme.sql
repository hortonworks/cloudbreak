-- // CB-16247 adding audit external id to the scheme.
-- Migration SQL that makes the change goes here.

ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS auditexternalid varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE user_preferences DROP COLUMN IF EXISTS auditexternalid;

