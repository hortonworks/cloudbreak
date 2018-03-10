-- // BUG-97815 add extendedBlueprintText to Cluster table
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster ADD extendedBlueprintText text;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster DROP COLUMN IF EXISTS extendedBlueprintText;

