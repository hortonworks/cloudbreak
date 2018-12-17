-- // BUG-116069 create datalakeResourceId in stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS datalakeresourceid BIGINT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN datalakeresourceid;
