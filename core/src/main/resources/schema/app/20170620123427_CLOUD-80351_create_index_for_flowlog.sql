-- // CLOUD-80351_create_index_for_flowlog
-- Migration SQL that makes the change goes here.

CREATE INDEX idx_flowlog_finalized ON flowlog (finalized);
CREATE INDEX idx_flowlog_finalized_stackid_flowtype ON flowlog (finalized, stackid, flowtype);

CREATE INDEX idx_flowchainlog_flowchainid_created ON flowchainlog (flowchainid, created);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX idx_flowlog_finalized;
DROP INDEX idx_flowlog_finalized_stackid_flowtype;

DROP INDEX idx_flowchainlog_flowchainid_created;
