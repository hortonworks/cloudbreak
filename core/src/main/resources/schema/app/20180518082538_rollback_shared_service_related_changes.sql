-- // rollback shared service related changes
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN blueprintcustomproperties TEXT;
ALTER TABLE cluster ADD COLUMN blueprintinputs TEXT;

-- //@UNDO
-- SQL to undo the change goes here.


