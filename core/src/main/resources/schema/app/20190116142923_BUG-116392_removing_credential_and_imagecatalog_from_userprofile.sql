-- // BUG-116392 removing credential and imagecatalog from userprofile
-- Migration SQL that makes the change goes here.
ALTER TABLE userprofile DROP COLUMN IF EXISTS credential_id;
ALTER TABLE userprofile DROP COLUMN IF EXISTS imagecatalog_id;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE userprofile ADD COLUMN IF NOT EXISTS credential_id bigint REFERENCES credential(id);
ALTER TABLE userprofile ADD COLUMN IF NOT EXISTS imagecatalog_id bigint REFERENCES imagecatalog(id);

