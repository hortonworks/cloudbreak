-- // CB-15622 Increase resourcereference length
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS resource ALTER COLUMN resourcereference SET DATA TYPE TEXT;


-- //@UNDO
-- SQL to undo the change goes here.

-- Nothing to undo