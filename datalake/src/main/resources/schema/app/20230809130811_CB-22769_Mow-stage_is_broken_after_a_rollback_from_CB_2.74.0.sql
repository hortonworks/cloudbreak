-- // CB-22769 Mow-stage is broken after a rollback from CB 2.74.0
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ALTER COLUMN createdatabase SET DEFAULT FALSE;

UPDATE sdxcluster
    SET createdatabase = FALSE
    WHERE createdatabase IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.
