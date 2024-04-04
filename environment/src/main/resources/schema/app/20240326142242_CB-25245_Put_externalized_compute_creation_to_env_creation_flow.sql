-- // CB-25245 -- Put externalized compute creation to env creation flow
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS create_compute_cluster boolean NOT NULL DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS create_compute_cluster;
