-- // CLOUD-67310 add usePrivateIpToTls field to SecurityConfig table
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig ADD COLUMN useprivateiptotls boolean NOT NULL DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig DROP COLUMN IF EXISTS useprivateiptotls;


