-- // CB-26141
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS architecture varchar(255);
ALTER TABLE clustertemplate DROP COLUMN IF EXISTS architecture;

-- //@UNDO
-- SQL to undo the change goes here.
