-- // CB-24984 -- Add default cluster field
-- Migration SQL that makes the change goes here.

ALTER TABLE externalized_compute_cluster ALTER COLUMN tags drop not null;

-- //@UNDO
-- SQL to undo the change goes here.

