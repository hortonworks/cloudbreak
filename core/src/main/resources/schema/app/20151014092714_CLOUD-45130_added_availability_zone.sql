-- // CLOUD-45130 added availability zone
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN availabilityZone text;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN availabilityZone;