-- // CB-14342
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS instancemetadata_instanceid
    ON instancemetadata (instanceid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS instancemetadata_instanceid;

