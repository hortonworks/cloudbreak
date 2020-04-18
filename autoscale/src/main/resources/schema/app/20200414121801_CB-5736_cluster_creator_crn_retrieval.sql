-- // CB-5736 cluster creator crn retrieval
-- Migration SQL that makes the change goes here.

ALTER TABLE clusterpertain ADD COLUMN IF NOT EXISTS usercrn VARCHAR(512);

-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE clusterpertain DROP COLUMN IF EXISTS usercrn;