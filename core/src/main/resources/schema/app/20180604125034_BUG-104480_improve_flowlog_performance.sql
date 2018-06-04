-- // BUG-104480 improve flowlog performance
-- Migration SQL that makes the change goes here.

CREATE INDEX idx_flowlog_statestatus ON flowlog (statestatus);

CREATE INDEX idx_flowlog_flowid ON flowlog (flowid);


-- //@UNDO
-- SQL to undo the change goes here.


DROP INDEX idx_flowlog_statestatus;

DROP INDEX idx_flowlog_flowid;