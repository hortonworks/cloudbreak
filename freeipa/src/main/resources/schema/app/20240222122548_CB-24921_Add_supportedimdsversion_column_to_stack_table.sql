-- // CB-24921 Add supportedimdsversion column to image table
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS supportedimdsversion VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS supportedimdsversion;
