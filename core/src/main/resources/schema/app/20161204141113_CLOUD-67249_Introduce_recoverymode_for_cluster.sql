-- // CLOUD-67249 Introduce recoverymode for cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN recoverymode varchar(255) DEFAULT 'MANUAL';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN recoverymode;
