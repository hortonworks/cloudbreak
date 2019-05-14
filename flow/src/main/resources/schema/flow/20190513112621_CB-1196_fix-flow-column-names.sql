-- // CB-1196_fix-flow-column-names
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog RENAME COLUMN stackid TO resourceid;
ALTER TABLE IF EXISTS flowlog add COLUMN resourcetype varchar(255) NULL;

CREATE INDEX IF NOT EXISTS idx_flowlog_resourceid_resourcetype ON flowlog USING btree (resourceid, resourcetype);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_flowlog_resourceid_resourcetype;

ALTER TABLE IF EXISTS flowlog DROP COLUMN resourcetype;
ALTER TABLE IF EXISTS flowlog RENAME COLUMN resourceid TO stackid;
