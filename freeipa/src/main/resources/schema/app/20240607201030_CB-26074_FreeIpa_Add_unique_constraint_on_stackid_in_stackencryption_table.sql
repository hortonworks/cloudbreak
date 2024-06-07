-- // CB-26074 FreeIpa: Add unique constraint on stackid in stackencryption table
-- Migration SQL that makes the change goes here.
ALTER TABLE stackencryption
ADD CONSTRAINT unique_stackid_for_stackencryption UNIQUE (stackid);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE stackencryption
DROP CONSTRAINT IF EXISTS unique_stackid_for_stackencryption;


