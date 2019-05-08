-- CB-1174 database server config environment IDs

ALTER TABLE ONLY databaseserverconfig ADD CONSTRAINT uk_databaseserverconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

ALTER TABLE databaseserverconfig ADD COLUMN environmentid VARCHAR(255);

UPDATE databaseserverconfig SET environmentid = 'unknown' WHERE environmentid IS NULL;

ALTER TABLE databaseserverconfig ALTER COLUMN environmentid SET NOT NULL;

CREATE INDEX IF NOT EXISTS databaseserverconfig_environmentid_idx ON databaseserverconfig(environmentid);

-- //@UNDO

DROP INDEX IF EXISTS databaseserverconfig_environmentid_idx;

ALTER TABLE databaseserverconfig DROP COLUMN environmentid;

ALTER TABLE ONLY databaseserverconfig DROP CONSTRAINT IF EXISTS uk_databaseserverconfig_deletiondate_workspace;
