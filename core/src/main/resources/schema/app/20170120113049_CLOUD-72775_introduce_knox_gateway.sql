-- // CLOUD-72775 introduce knox gateway
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN enableknoxgateway BOOLEAN NOT NULL DEFAULT FALSE;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN enableknoxgateway;
