-- // additional-fields-for-filesystem-table
-- Migration SQL that makes the change goes here.
ALTER TABLE filesystem
ADD COLUMN IF NOT EXISTS publicinaccount boolean      NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS owner           VARCHAR(255) NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS description     VARCHAR(255) DEFAULT '',
ADD COLUMN IF NOT EXISTS account         VARCHAR(255) NOT NULL DEFAULT '';


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE filesystem
DROP COLUMN IF EXISTS publicinaccount,
DROP COLUMN IF EXISTS owner,
DROP COLUMN IF EXISTS description,
DROP COLUMN IF EXISTS account;
