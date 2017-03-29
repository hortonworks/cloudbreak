
-- // CLOUD-6567 create sign cert
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway ADD COLUMN signcert TEXT;
ALTER TABLE gateway ADD COLUMN signpub TEXT;
ALTER TABLE securityconfig ADD COLUMN knoxmastersecret varchar(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE gateway DROP COLUMN signcert;
ALTER TABLE gateway DROP COLUMN signpub;
ALTER TABLE securityconfig DROP COLUMN knoxmastersecret;