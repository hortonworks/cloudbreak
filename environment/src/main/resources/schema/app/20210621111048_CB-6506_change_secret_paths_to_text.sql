-- // CB-6506 Change secret paths to text
-- Migration SQL that makes the change goes here.

ALTER TABLE proxyconfig
    ALTER COLUMN username TYPE TEXT,
    ALTER COLUMN password TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

-- Left blank, can't rollback if longer data was inserted therefore we leave columns as text
