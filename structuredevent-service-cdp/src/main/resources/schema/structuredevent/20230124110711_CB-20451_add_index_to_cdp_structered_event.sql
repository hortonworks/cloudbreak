-- // CB-20451 Add index to cdp_structured_event for accountid, timestamp
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_cdp_structured_event_accountid_timestamp ON cdp_structured_event (accountid, timestamp);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_cdp_structured_event_accountid_timestamp;
