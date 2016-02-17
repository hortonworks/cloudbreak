-- // CLOUD-45388 aws in separated modul
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY credential DROP CONSTRAINT fk_credential_temporaryawscredentials_accesskeyid;
ALTER TABLE credential DROP COLUMN temporaryawscredentials_accesskeyid;
DROP TABLE temporaryawscredentials;


-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE temporaryawscredentials (
    accesskeyid character varying(255) NOT NULL,
    secretaccesskey character varying(255),
    sessiontoken text,
    validuntil bigint NOT NULL
);
ALTER TABLE credential ADD COLUMN temporaryawscredentials_accesskeyid character varying(255);
ALTER TABLE ONLY credential ADD CONSTRAINT fk_credential_temporaryawscredentials_accesskeyid FOREIGN KEY (temporaryawscredentials_accesskeyid) REFERENCES temporaryawscredentials(accesskeyid);