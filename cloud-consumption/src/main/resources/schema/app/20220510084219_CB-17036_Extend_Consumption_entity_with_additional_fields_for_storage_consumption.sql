-- // CB-17036 Extend Consumption entity with additional fields for storage consumption
-- Migration SQL that makes the change goes here.

DELETE FROM consumption;

ALTER TABLE consumption ADD IF NOT EXISTS environmentCrn VARCHAR(255) NOT NULL;
ALTER TABLE consumption ADD IF NOT EXISTS monitoredResourceType VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE consumption ADD IF NOT EXISTS monitoredResourceCrn VARCHAR(255) NOT NULL;
ALTER TABLE consumption ADD IF NOT EXISTS consumptionType VARCHAR(255) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE consumption ADD IF NOT EXISTS storageLocation VARCHAR(255);

CREATE INDEX IF NOT EXISTS consumption_monitoredcrn_consumptiontype_idx ON consumption USING btree (monitoredResourceCrn, consumptionType);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS consumption_monitoredcrn_consumptiontype_idx;
ALTER TABLE consumption DROP COLUMN IF EXISTS environmentCrn;
ALTER TABLE consumption DROP COLUMN IF EXISTS monitoredResourceType;
ALTER TABLE consumption DROP COLUMN IF EXISTS monitoredResourceCrn;
ALTER TABLE consumption DROP COLUMN IF EXISTS consumptionType;
ALTER TABLE consumption DROP COLUMN IF EXISTS storageLocation;
