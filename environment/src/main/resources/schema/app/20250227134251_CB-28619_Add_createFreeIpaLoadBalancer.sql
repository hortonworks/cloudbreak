-- // CB-28619 add field to control FreeIpa loadbalancer creation
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeipaloadbalancer varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS freeipaloadbalancer;