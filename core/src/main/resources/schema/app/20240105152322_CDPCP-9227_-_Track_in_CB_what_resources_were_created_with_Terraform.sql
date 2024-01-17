-- // CDPCP-9227 - Track in CB what resources were created with Terraform
-- Migration SQL that makes the change goes here.
ALTER TABLE stack ADD COLUMN IF NOT EXISTS creator_client VARCHAR(128) DEFAULT 'No Info';
UPDATE stack SET creator_client = 'No Info' WHERE creator_client IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE stack DROP COLUMN IF EXISTS creator_client;

