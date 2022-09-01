-- // CB-17015_Add_InstanceStorageSize_to_Template
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS instancestoragesize INTEGER;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS instancestoragesize;
