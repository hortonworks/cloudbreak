-- // CB-6007 Get rid of the generated instancegroup queries during sync
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS instancemetadata_fqdn_idx ON instancemetadata (discoveryfqdn);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS instancemetadata_fqdn_idx;
