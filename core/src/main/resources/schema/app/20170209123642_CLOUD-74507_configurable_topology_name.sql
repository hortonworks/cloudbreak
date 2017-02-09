-- // CLOUD-74507 configurable topology name
-- Migration SQL that makes the change goes here.


ALTER TABLE cluster ADD COLUMN knoxtopologyname TEXT;

UPDATE cluster SET knoxtopologyname = 'hdc' WHERE enableknoxgateway = true;

-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE cluster DROP COLUMN knoxtopologyname;
