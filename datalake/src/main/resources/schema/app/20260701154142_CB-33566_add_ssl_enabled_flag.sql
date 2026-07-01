-- // CB-33566 add ssl enabled flag
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxdatabase ADD COLUMN IF NOT EXISTS dbSslEnabled BOOLEAN;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxdatabase DROP COLUMN IF EXISTS dbSslEnabled;

