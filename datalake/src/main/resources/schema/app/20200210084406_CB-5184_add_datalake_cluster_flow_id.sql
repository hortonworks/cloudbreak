-- // CB-5184 Extend Responses which starts a Flow internally
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS lastcbflowid character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS lastcbflowid;