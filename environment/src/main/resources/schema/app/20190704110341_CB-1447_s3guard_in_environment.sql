-- // CB-1447 adding S3Guard table name to environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS s3guarddynamotable VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS s3guarddynamotable;
