-- // CB-33218 Add index on (resourcecrn, timestamp) to cdp_structured_event
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_cdp_structured_event_resourcecrn_timestamp ON cdp_structured_event (resourcecrn, timestamp);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_cdp_structured_event_resourcecrn_timestamp;
