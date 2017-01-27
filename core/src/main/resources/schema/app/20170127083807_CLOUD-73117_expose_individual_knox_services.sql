-- // CLOUD-73117 expose individual knox services
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN exposedknoxservices TEXT;

-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE cluster DROP COLUMN exposedknoxservices;