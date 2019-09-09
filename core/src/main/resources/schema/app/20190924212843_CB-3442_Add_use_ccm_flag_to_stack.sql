-- // CB-3442 Add use_ccm flag to stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack
    ADD COLUMN useccm BOOLEAN;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack
    DROP COLUMN useccm;
