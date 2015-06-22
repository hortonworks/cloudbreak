-- // CLOUD-840 Added new column for marking deleted credentials
-- Migration SQL that makes the change goes here.
ALTER TABLE credential ADD COLUMN archived boolean DEFAULT false;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE credential DROP COLUMN archived;

