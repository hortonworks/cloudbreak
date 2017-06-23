-- // CLOUD-80499 Allow multiple gateway setup
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN multigateway boolean default false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS multigateway;
