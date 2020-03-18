-- // CB-6084 add verify column to credential
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD COLUMN IF NOT EXISTS verifyPermissions boolean NOT NULL DEFAULT true ;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential DROP COLUMN IF EXISTS verifyPermissions;

