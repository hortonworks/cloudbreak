-- // CB-1364 Database uniqueness constraint
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY databaseconfig
    DROP CONSTRAINT IF EXISTS uk_databaseconfig_deletiondate_environment;

-- PostgreSQL unique constraints always treat NULL values as unequal
-- Therefore, use different constraints depending on value of deletiontimestamp

CREATE UNIQUE INDEX uk_databaseconfig_deletionTimestamp_environment
    ON databaseconfig (environment_id, name, deletionTimestamp)
    WHERE deletionTimestamp IS NOT NULL;
CREATE UNIQUE INDEX uk_databaseconfig_nulldeletionTimestamp_environment
    ON databaseconfig (environment_id, name)
    WHERE deletionTimestamp IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS uk_databaseconfig_nulldeletionTimestamp_environment;
DROP INDEX IF EXISTS uk_databaseconfig_deletionTimestamp_environment;

ALTER TABLE databaseconfig
    ADD CONSTRAINT uk_databaseconfig_deletiondate_environment UNIQUE (name, deletionTimestamp, environment_id);
