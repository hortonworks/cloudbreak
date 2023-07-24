-- // CB-22525 Increase resource table resourcereference size
-- Migration SQL that makes the change goes here.

ALTER TABLE resource ALTER COLUMN resourcereference TYPE VARCHAR(1024);

-- //@UNDO
-- SQL to undo the change goes here.


