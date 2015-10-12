-- // CLOUD-45388 aws in separated modul
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY credential DROP CONSTRAINT fk_credential_temporaryawscredentials_accesskeyid;
ALTER TABLE credential DROP COLUMN temporaryawscredentials_accesskeyid;
DROP TABLE temporaryawscredentials;


-- //@UNDO
-- SQL to undo the change goes here.

