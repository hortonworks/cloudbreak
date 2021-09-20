-- // CB-14194 adding multiaz field to datalake object to request a multiaz aws datalake
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS enablemultiaz boolean;
UPDATE sdxcluster SET enablemultiaz = FALSE WHERE enablemultiaz is NULL;
ALTER TABLE sdxcluster ALTER enablemultiaz SET DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS enablemultiaz;

