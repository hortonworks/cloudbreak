-- // CB-18697 remove usage of creator to avoid ums notfound issues
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint DROP COLUMN IF EXISTS creator;
ALTER TABLE customimage DROP COLUMN IF EXISTS creator;
ALTER TABLE vmimage DROP COLUMN IF EXISTS creator;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS creator character varying(255);
ALTER TABLE customimage ADD COLUMN IF NOT EXISTS creator character varying(255);
ALTER TABLE vmimage ADD COLUMN IF NOT EXISTS creator character varying(255);
