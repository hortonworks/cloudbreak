-- // CB-14578 create CCM Parameters column
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS ccmparameters text;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE stack DROP COLUMN IF EXISTS ccmparameters;
