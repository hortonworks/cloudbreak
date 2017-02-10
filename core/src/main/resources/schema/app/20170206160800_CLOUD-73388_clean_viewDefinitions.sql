-- // CLOUD-73388 clean viewDefinitions
-- Migration SQL that makes the change goes here.

UPDATE cluster SET attributes = NULL;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE cluster SET attributes = NULL;