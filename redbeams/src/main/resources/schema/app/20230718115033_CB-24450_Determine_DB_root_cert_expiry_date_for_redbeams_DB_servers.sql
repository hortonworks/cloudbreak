-- // CB-22525 Increase resource table resourcereference size
-- Migration SQL that makes the change goes here.

ALTER TABLE sslconfig
    ADD COLUMN IF NOT EXISTS sslcertificateexpirationdate BIGINT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sslconfig
    DROP COLUMN IF EXISTS sslcertificateexpirationdate;