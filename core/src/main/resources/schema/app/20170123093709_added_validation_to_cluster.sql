-- // added validation to cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN  topologyvalidation BOOLEAN DEFAULT TRUE NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN topologyvalidation;
