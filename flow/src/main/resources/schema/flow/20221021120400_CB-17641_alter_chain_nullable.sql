-- // CB-17641 Remove NOT NULL constraint from chain table, preparing for removal
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowchainlog ALTER COLUMN chain DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.
-- cannot put back constraint
