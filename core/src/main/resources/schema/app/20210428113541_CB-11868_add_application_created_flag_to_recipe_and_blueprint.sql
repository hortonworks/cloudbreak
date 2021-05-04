-- // CB-11868 Add application created flag to recipe and blueprint
-- Migration SQL that makes the change goes here.
ALTER TABLE recipe ADD COLUMN creationtype CHARACTER VARYING(255);

UPDATE recipe SET creationtype='USER';

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE recipe DROP COLUMN creationtype;
