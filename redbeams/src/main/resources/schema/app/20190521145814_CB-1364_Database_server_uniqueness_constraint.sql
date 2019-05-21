-- // CB-1364 Database server uniqueness constraint
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY databaseserverconfig
    DROP CONSTRAINT IF EXISTS uk_databaseserverconfig_deletiondate_workspace;

-- PostgreSQL unique constraints always treat NULL values as unequal
-- Therefore, use different constraints depending on value of deletiontimestamp

CREATE UNIQUE INDEX uk_databaseserverconfig_deletionTimestamp_environment
    ON databaseserverconfig (environmentid, name, deletionTimestamp)
    WHERE deletionTimestamp IS NOT NULL;
CREATE UNIQUE INDEX uk_databaseserverconfig_nulldeletionTimestamp_environment
    ON databaseserverconfig (environmentid, name)
    WHERE deletionTimestamp IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS uk_databaseserverconfig_nulldeletionTimestamp_environment;
DROP INDEX IF EXISTS uk_databaseserverconfig_deletionTimestamp_environment;

ALTER TABLE ONLY databaseserverconfig
    ADD CONSTRAINT uk_databaseserverconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);
