-- // add cluster variant
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS variant TEXT DEFAULT 'AMBARI';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS variant;