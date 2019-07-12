-- // CB-2365 Add a resource CRN to the DBStack
-- Migration SQL that makes the change goes here.

ALTER TABLE dbStack
    ADD COLUMN resourcecrn TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE dbStack
    DROP COLUMN resourcecrn;

