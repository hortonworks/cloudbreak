-- // CB-35498 Disable notification by default for every stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ALTER COLUMN notificationstate SET DEFAULT 'DISABLED';
UPDATE stack SET notificationstate = 'DISABLED' WHERE notificationstate IS DISTINCT FROM 'DISABLED';

-- //@UNDO
-- SQL to undo the change goes here.
-- The previous per-stack values are not recoverable, so only the column default is restored.

ALTER TABLE stack ALTER COLUMN notificationstate SET DEFAULT 'ENABLED';
