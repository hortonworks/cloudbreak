-- // CB-22002 Remove deprecated db related fields from Stack
-- Migration SQL that makes the change goes here.

ALTER TABLE database ADD COLUMN IF NOT EXISTS stack_id BIGINT;

INSERT INTO database (stack_id, externaldatabaseavailabilitytype, externaldatabaseengineversion)
    SELECT stack.id, stack.externaldatabasecreationtype, stack.externaldatabaseengineversion
        FROM stack WHERE database_id IS NULL;

UPDATE stack
    SET database_id = database.id
    FROM database
    WHERE database_id IS NULL AND database.stack_id = stack.id;

UPDATE database
    SET externaldatabaseengineversion = stack.externaldatabaseengineversion
    FROM stack
    WHERE stack.database_id = database.id AND
        stack.externaldatabaseengineversion IS NOT NULL AND
        database.externaldatabaseengineversion != stack.externaldatabaseengineversion;

ALTER TABLE database DROP COLUMN IF EXISTS stack_id;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE stack
    SET
        externaldatabasecreationtype = database.externaldatabaseavailabilitytype,
        externaldatabaseengineversion = database.externaldatabaseengineversion
    FROM database
    WHERE stack.database_id = database.id;
