-- // CB-25017 adding url for private control planes
-- Migration SQL that makes the change goes here.

ALTER TABLE private_control_plane ADD COLUMN IF NOT EXISTS privateCloudAccountId text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE private_control_plane DROP COLUMN IF EXISTS privateCloudAccountId;