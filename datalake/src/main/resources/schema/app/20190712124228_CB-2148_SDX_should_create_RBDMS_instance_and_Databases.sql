-- // CB-2148 SDX should create RBDMS instance and Databases
-- Migration SQL that makes the change goes here.


ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS createdatabase boolean NOT NULL DEFAULT false;
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS databasecrn VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS createdatabase;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS databasecrn;

