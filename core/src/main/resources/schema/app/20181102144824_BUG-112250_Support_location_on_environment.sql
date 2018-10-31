-- // BUG-112250 Support location on environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS location varchar(255);
ALTER TABLE environment ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE environment ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS location;
ALTER TABLE environment DROP COLUMN IF EXISTS longitude;
ALTER TABLE environment DROP COLUMN IF EXISTS latitude;
