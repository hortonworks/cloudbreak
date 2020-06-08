-- // CB-7326 extend Cluster entity with keystore and truststore pwd fields
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS keystorepwd text;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS truststorepwd text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS keystorepwd;
ALTER TABLE cluster DROP COLUMN IF EXISTS truststorepwd;
