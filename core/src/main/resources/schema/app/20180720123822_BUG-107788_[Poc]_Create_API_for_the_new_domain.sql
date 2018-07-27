-- // BUG-107788 [Poc] Create API for the new domain
-- Migration SQL that makes the change goes here.

ALTER TABLE organization ADD COLUMN description TEXT;
ALTER TABLE tenant ADD COLUMN description TEXT;

INSERT INTO tenant (name, description) values ('DEFAULT', 'Default Tenant') ON CONFLICT DO NOTHING;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE organization DROP COLUMN IF EXISTS description;
ALTER TABLE tenant DROP COLUMN IF EXISTS description;
