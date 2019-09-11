-- // CB-3270_save_private_subnet_creation_flag
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD COLUMN IF NOT EXISTS verificationStatusText text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential DROP COLUMN IF EXISTS verificationStatusText;
