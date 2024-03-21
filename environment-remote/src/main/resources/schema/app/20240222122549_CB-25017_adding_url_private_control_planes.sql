-- // CB-25017 adding url for private control planes
-- Migration SQL that makes the change goes here.

ALTER TABLE private_control_plane ADD COLUMN IF NOT EXISTS url text NOT NULL DEFAULT 'http://localhost';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE private_control_plane DROP COLUMN IF EXISTS url;