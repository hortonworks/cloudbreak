-- // CB-2536 extendig filesystem with cloud storage

-- Migration SQL that makes the change goes here.

ALTER TABLE filesystem ADD COLUMN IF NOT EXISTS cloudstorage TEXT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE filesystem DROP COLUMN IF EXISTS cloudstorage;
