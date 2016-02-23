-- // CLOUD-50403 cluster certdir and stack cloudplatform
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN cloudplatform VARCHAR(255);

UPDATE stack s SET cloudplatform = c.cloudplatform FROM credential c WHERE s.credential_id = c.id AND s.credential_id IS NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN cloudplatform;