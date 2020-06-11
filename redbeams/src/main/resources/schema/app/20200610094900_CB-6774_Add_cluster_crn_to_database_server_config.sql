-- // CB-6774 Add the cluster CRN to the DatabaseServerConfig
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseserverconfig
    ADD COLUMN clustercrn TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE databaseserverconfig
    DROP COLUMN clustercrn;