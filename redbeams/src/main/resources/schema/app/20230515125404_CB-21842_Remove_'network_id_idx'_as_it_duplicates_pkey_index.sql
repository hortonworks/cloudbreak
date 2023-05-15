-- // CB-21842 Remove 'network_id_idx' as it duplicates pkey index
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS network_id_idx;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE UNIQUE INDEX IF NOT EXISTS network_id_idx ON network(id);
