-- // CB-18783 Drop old payload fields
-- Migration SQL that makes the change goes here.

ALTER TABLE flowlog DROP COLUMN IF EXISTS payload, DROP COLUMN IF EXISTS variables;
ALTER TABLE flowchainlog DROP COLUMN IF EXISTS chain, DROP COLUMN IF EXISTS triggerevent;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flowlog ADD COLUMN IF NOT EXISTS payload text, ADD COLUMN IF NOT EXISTS variables text;
ALTER TABLE flowchainlog ADD COLUMN IF NOT EXISTS chain text, ADD COLUMN IF NOT EXISTS triggerevent text;
