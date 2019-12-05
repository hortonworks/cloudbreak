-- // CB-4548 alter stack add column minasshdserviceid
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS minasshdserviceid VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS minasshdserviceid;
