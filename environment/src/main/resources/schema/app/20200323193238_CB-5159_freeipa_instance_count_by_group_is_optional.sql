-- // CB-1617 freeipa creation is optional
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeIpaInstanceCountByGroup integer;
UPDATE environment SET freeIpaInstanceCountByGroup = 1 WHERE freeIpaInstanceCountByGroup IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS freeIpaInstanceCountByGroup;

