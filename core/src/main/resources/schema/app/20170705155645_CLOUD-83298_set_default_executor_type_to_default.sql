-- // CLOUD-83298 set default executor type to default
-- Migration SQL that makes the change goes here.

UPDATE cluster SET executortype = 'DEFAULT' WHERE executortype = 'SIMPLE';
ALTER TABLE cluster ALTER COLUMN executortype SET DEFAULT 'DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.


-- no need to do an undo