-- // CB-3358 add user facing cert and private key to security config
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig ADD COLUMN userFacingCert text;
ALTER TABLE securityconfig ADD COLUMN userFacingKey text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig DROP COLUMN userFacingCert;
ALTER TABLE securityconfig DROP COLUMN userFacingKey;

