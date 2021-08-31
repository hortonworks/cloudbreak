-- // CB-13731 add root trigger event column to flow chain log table
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowchainlog ADD COLUMN IF NOT EXISTS triggerevent text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS flowchainlog DROP COLUMN IF EXISTS triggerevent;