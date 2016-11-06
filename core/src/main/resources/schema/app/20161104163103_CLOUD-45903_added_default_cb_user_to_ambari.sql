-- // CLOUD-45903 added default cb user to ambari
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN cloudbreakAmbariUser varchar(255);
ALTER TABLE cluster ADD COLUMN cloudbreakAmbariPassword varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN cloudbreakAmbariUser;
ALTER TABLE cluster DROP COLUMN cloudbreakAmbariPassword;
