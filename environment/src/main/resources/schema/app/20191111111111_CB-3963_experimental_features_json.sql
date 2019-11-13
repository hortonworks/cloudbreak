-- // CB-3963 Experimental features json object in Environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS experimentalfeatures text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS experimentalfeatures;
