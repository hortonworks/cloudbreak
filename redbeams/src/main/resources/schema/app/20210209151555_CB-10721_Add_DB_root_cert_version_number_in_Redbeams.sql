-- // CB-10721 Add DB root cert version number in Redbeams
-- Migration SQL that makes the change goes here.

ALTER TABLE sslconfig
    ADD COLUMN IF NOT EXISTS sslcertificateactiveversion INT4;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sslconfig
    DROP COLUMN IF EXISTS sslcertificateactiveversion;
