-- // CLOUD-43947: extend resource with reference and type
-- Migration SQL that makes the change goes here.

ALTER TABLE resource ADD COLUMN resourceReference CHARACTER VARYING (255);
ALTER TABLE resource ADD COLUMN resourceStatus CHARACTER VARYING (255) DEFAULT('CREATED');

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE resource DROP COLUMN resourceReference;
ALTER TABLE resource DROP COLUMN resourceStatus;
