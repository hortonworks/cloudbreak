-- // CB-17015_Add_InstanceStorageSize_to_Template
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS instancestoragesize INTEGER;
UPDATE template SET instancestoragesize = 0 WHERE instancestoragesize IS NULL;
ALTER TABLE template ALTER instancestoragesize SET DEFAULT 0;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS instancestoragesize;
