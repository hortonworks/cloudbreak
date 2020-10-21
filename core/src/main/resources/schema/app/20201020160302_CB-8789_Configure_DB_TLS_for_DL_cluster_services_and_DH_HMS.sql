-- // CB-8789 Configure DB TLS for DL cluster services and DH HMS
-- Migration SQL that makes the change goes here.

ALTER TABLE rdsconfig
    ADD COLUMN IF NOT EXISTS sslmode VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE rdsconfig
    DROP COLUMN IF EXISTS sslmode;
