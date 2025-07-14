-- // CB-29972 Environmenttype should not be null in database
-- Migration SQL that makes the change goes here.

UPDATE environment SET environmenttype='PUBLIC_CLOUD' where environmenttype IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.


