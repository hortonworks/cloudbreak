-- // CB-1870 remove accessCidr from the sdxcluster table
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS accessCidr;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster ADD COLUMN accessCidr CHARACTER VARYING(255);

UPDATE sdxcluster SET accessCidr = '0.0.0.0/0';

ALTER TABLE sdxcluster ALTER COLUMN accessCidr SET NOT NULL;
