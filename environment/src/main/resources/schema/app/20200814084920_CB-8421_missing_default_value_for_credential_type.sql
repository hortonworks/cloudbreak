-- // CB-8421 missing default value for credential type
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ALTER COLUMN type SET DEFAULT 'ENVIRONMENT';
UPDATE credential SET type='ENVIRONMENT' WHERE type IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential ALTER type DROP DEFAULT;
