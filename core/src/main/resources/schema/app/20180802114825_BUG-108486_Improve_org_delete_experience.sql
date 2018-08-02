-- // BUG-108486 Improve org delete experience
-- Migration SQL that makes the change goes here.

ALTER TABLE organization ADD COLUMN status VARCHAR (255) DEFAULT 'ACTIVE';
ALTER TABLE organization ADD COLUMN deletionTimestamp BIGINT;

ALTER TABLE organization DROP CONSTRAINT IF EXISTS org_in_tenant_unique;
ALTER TABLE organization ADD CONSTRAINT org_in_tenant_deletiondate_unique UNIQUE (name, deletionTimestamp, tenant_id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE organization DROP COLUMN IF EXISTS status;
ALTER TABLE organization DROP COLUMN IF EXISTS deletionTimestamp;

ALTER TABLE organization DROP CONSTRAINT IF EXISTS org_in_tenant_deletiondate_unique;
ALTER TABLE organization ADD CONSTRAINT org_in_tenant_unique UNIQUE (name, tenant_id);