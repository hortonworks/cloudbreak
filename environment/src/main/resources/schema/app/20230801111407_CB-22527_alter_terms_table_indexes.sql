-- // CB-22527 alter terms table indexes
-- Migration SQL that makes the change goes here.

ALTER TABLE terms ALTER COLUMN resourcecrn DROP NOT NULL;

DROP INDEX IF EXISTS terms_id_idx;

DROP INDEX IF EXISTS terms_accountid_idx;

CREATE UNIQUE INDEX IF NOT EXISTS terms_accountid_idx ON terms (accountid);

-- //@UNDO
-- SQL to undo the change goes here.


