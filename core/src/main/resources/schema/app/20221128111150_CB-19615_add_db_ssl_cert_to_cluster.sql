-- // CB-19615 Update the cert on the Cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS dbSslRootCertBundle TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS dbSslEnabled bool default false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS dbSslRootCertBundle;
ALTER TABLE cluster DROP COLUMN IF EXISTS dbSslEnabled;