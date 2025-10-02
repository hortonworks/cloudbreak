-- // CB-3358 Add alternative user facing certificates
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig ADD COLUMN alternativeUserFacingCert text;
ALTER TABLE securityconfig ADD COLUMN alternativeUserFacingKey text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig DROP COLUMN alternativeUserFacingCert;
ALTER TABLE securityconfig DROP COLUMN alternativeUserFacingKey;

