-- // CB-14322
-- Migration SQL that makes the change goes here.

CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accid_envcrn_detached_deleted_is_null
    ON sdxcluster (accountid, envCrn, deleted, detached)
    WHERE deleted IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accid_crn_detached_deleted_is_null
    ON sdxcluster (accountid, crn, deleted, detached)
    WHERE deleted IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accid_envcrn_detached_deleted_is_not_null
    ON sdxcluster (accountid, envCrn, deleted, detached)
    WHERE deleted IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accid_crn_detached_deleted_is_not_null
    ON sdxcluster (accountid, crn, deleted, detached)
    WHERE deleted IS NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS unq_index_sdxcluster_accid_envcrn_detached_deleted_is_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accid_crn_detached_deleted_is_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accid_envcrn_detached_deleted_is_not_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accid_crn_detached_deleted_is_not_null;


