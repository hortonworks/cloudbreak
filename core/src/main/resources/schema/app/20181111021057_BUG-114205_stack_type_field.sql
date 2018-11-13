-- // BUG-114205 stack type field
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS type VARCHAR(255) DEFAULT 'WORKLOAD';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS type;