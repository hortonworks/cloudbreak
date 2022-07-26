-- // CB-17459 add columns to store Jackson-serialized objects too
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog ADD COLUMN IF NOT EXISTS payloadjackson text;
ALTER TABLE IF EXISTS flowlog ADD COLUMN IF NOT EXISTS variablesjackson text;

ALTER TABLE IF EXISTS flowchainlog ADD COLUMN IF NOT EXISTS triggereventjackson text;
ALTER TABLE IF EXISTS flowchainlog ADD COLUMN IF NOT EXISTS chainjackson text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS flowlog DROP COLUMN IF EXISTS payloadjackson;
ALTER TABLE IF EXISTS flowlog DROP COLUMN IF EXISTS variablesjackson;

ALTER TABLE IF EXISTS flowchainlog DROP COLUMN IF EXISTS triggereventjackson;
ALTER TABLE IF EXISTS flowchainlog DROP COLUMN IF EXISTS chainjackson;
