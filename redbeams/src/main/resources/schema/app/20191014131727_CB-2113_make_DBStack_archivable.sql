-- // CB-2113 make DBStack archivable
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS dbstack_name_idx;
ALTER TABLE dbstack
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    ADD CONSTRAINT uk_dbstack_name_deletiondate UNIQUE (name, deletionTimestamp);

-- //@UNDO
-- SQL to undo the change goes here.
DELETE FROM dbstack WHERE archived=true;
ALTER TABLE dbstack
    DROP CONSTRAINT uk_dbstack_name_deletiondate,
    DROP COLUMN archived,
    DROP COLUMN deletionTimestamp;
CREATE INDEX IF NOT EXISTS dbstack_name_idx ON dbstack(name);


