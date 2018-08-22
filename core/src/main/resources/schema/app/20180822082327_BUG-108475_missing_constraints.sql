-- // BUG-108475_missing_constraints
-- Migration SQL that makes the change goes here.

ALTER TABLE network DROP CONSTRAINT IF EXISTS uk_networknameinaccount;

-- //@UNDO
-- SQL to undo the change goes here.

