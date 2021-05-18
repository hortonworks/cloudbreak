-- // CB-12613 Add TemporaryStorage to Template
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS temporarystorage VARCHAR (255);
UPDATE template SET temporarystorage = 'ATTACHED_VOLUMES' WHERE temporarystorage IS NULL;
ALTER TABLE template ALTER temporarystorage SET DEFAULT 'ATTACHED_VOLUMES';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS temporarystorage;