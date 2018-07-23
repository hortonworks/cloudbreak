-- // BUG-107788 [Poc] Create API for the new domain
-- Migration SQL that makes the change goes here.

ALTER TABLE organization ADD COLUMN description TEXT;
ALTER TABLE tenant ADD COLUMN description TEXT;

INSERT INTO tenant (name, description) values ('DEFAULT', 'Default tenant for PoC.');

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE organization DROP COLUMN IF EXISTS description;
ALTER TABLE tenant DROP COLUMN IF EXISTS description;

DELETE FROM tenant where name = 'DEFAULT';
