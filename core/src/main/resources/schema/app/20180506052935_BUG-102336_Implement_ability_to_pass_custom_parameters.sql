-- // BUG-102336 Implement ability to pass custom parameters
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN inputs text;
ALTER TABLE cluster DROP COLUMN IF EXISTS blueprintinputs;
ALTER TABLE cluster DROP COLUMN IF EXISTS blueprintcustomproperties;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS inputs;
ALTER TABLE cluster ADD COLUMN blueprintcustomproperties TEXT;
ALTER TABLE cluster ADD COLUMN blueprintinputs TEXT;