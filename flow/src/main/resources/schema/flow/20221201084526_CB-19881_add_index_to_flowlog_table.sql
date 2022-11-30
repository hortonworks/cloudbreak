-- // CB-19881 Add index to flowlog table
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_flowlog_flowchainid
    ON flowlog (flowchainid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_flowlog_flowchainid;