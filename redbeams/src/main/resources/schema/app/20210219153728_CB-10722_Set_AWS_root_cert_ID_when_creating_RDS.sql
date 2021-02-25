-- // CB-10722 Set AWS root cert ID when creating RDS
-- Migration SQL that makes the change goes here.

ALTER TABLE sslconfig
    ADD COLUMN IF NOT EXISTS sslcertificateactivecloudprovideridentifier VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sslconfig
    DROP COLUMN IF EXISTS sslcertificateactivecloudprovideridentifier;
