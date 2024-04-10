-- // CB-24984 -- Add default cluster field
-- Migration SQL that makes the change goes here.

ALTER TABLE externalized_compute_cluster ADD COLUMN IF NOT EXISTS defaultcluster boolean NOT NULL DEFAULT false;

UPDATE externalized_compute_cluster SET defaultcluster = true;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE externalized_compute_cluster DROP COLUMN IF EXISTS defaultcluster;
