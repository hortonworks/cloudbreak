-- // BUG-72792 regions should be nullable
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ALTER COLUMN region DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack ALTER COLUMN region SET NOT NULL;
