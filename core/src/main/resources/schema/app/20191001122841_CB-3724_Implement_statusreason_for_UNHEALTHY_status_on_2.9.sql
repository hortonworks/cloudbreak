-- // CB-3724 Implement statusreason for UNHEALTHY status on 2.9
-- Migration SQL that makes the change goes here.

alter table hostmetadata ADD COLUMN IF NOT EXISTS statusReason TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

alter table hostmetadata DROP COLUMN IF EXISTS statusReason;

