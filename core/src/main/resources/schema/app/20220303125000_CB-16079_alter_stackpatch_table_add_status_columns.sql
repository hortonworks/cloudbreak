-- // CB-16079
-- Migration SQL that makes the change goes here.

ALTER TABLE stackpatch ADD COLUMN IF NOT EXISTS status CHARACTER VARYING(255);
UPDATE stackpatch SET status = 'FIXED' WHERE status IS NULL;
ALTER TABLE stackpatch ALTER COLUMN status SET NOT NULL;
ALTER TABLE stackpatch ALTER COLUMN status SET DEFAULT 'FIXED';

ALTER TABLE stackpatch ADD COLUMN IF NOT EXISTS status_reason TEXT;

-- //@UNDO
-- SQL to undo the change goes here.