-- // CB-1646 add resourcecrn to stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN resourcecrn VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN resourcecrn;
