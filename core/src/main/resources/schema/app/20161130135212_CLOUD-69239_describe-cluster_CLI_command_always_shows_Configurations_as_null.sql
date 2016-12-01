-- // CLOUD-69239 describe-cluster CLI command always shows Configurations as null
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD  blueprintcustomproperties text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN blueprintcustomproperties;


