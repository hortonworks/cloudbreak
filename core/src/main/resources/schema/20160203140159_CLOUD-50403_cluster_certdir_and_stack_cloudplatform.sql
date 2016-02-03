-- // CLOUD-50403 cluster certdir and stack cloudplatform
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN certdir VARCHAR(255);
ALTER TABLE stack ADD COLUMN cloudplatform VARCHAR(255);
-- // TODO: migrate existing clusters certdir and cloudplatform

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN certdir;
ALTER TABLE stack DROP COLUMN cloudplatform;