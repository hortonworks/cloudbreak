-- // CB-3336 Ensure that 'unhealthy' status contains more information
-- Migration SQL that makes the change goes here.

alter table hostmetadata ADD COLUMN IF NOT EXISTS statusReason TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

alter table hostmetadata DROP COLUMN IF EXISTS statusReason;

