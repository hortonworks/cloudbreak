-- // CB-21843 Remove 'sslconfig_id_idx' as it duplicates pkey index
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS sslconfig_id_idx;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE UNIQUE INDEX IF NOT EXISTS sslconfig_id_idx ON sslconfig(id);
