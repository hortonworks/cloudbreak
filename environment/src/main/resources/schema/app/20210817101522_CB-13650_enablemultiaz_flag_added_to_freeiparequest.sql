-- // CB-13650 Integrate Multi AZ with FreeIPA controller layer
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeipaenablemultiaz boolean NOT NULL DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS freeipaenablemultiaz;
