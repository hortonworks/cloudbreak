-- // CB-2778 cleanup not null for type
-- Migration SQL that makes the change goes here.


ALTER TABLE filesystem ALTER COLUMN type DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE filesystem ALTER COLUMN type SET NOT NULL;
