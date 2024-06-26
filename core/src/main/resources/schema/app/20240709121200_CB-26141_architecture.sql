-- // CB-26141
-- Migration SQL that makes the change goes here.

ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS architecture varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.
