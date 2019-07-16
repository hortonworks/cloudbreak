-- // CB-2411 added tunnel type
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS tunnel varchar(255) NOT NULL DEFAULT 'DIRECT';

UPDATE environment SET tunnel = 'DIRECT';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS tunnel;

