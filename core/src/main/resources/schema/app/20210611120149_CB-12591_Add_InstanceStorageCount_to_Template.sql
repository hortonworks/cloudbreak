-- // CB-12591 Add InstanceStorageCount to Template
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS instancestoragecount INTEGER;
UPDATE template SET instancestoragecount = 0 WHERE instancestoragecount IS NULL;
ALTER TABLE template ALTER instancestoragecount SET DEFAULT 0;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS instancestoragecount;
