-- // CB-32552 Staleness state sync
-- Migration SQL that makes the change goes here.
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS configstalenessstate VARCHAR(255) NOT NULL DEFAULT 'UP_TO_DATE';

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS configstalenessdetails TEXT;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS configstalenessstate;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS configstalenessdetails;
