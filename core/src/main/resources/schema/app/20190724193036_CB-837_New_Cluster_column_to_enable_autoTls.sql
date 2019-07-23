-- // CB-837 New Cluster column to enable autoTls
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS autotlsenabled boolean NOT NULL DEFAULT FALSE;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS autotlsenabled;
