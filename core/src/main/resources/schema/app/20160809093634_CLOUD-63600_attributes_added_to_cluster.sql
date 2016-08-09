-- // CLOUD-63600 attributes added to cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN attributes TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN attributes;

