-- // CB-17527 remove usage of initiatoruser in sdx service in order to avoid ums notfound issues
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS initiatorUserCrn;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS initiatorUserCrn character varying(255);
