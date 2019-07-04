-- // CB-1863 Search by envCrn and ClusterCrn
-- Migration SQL that makes the change goes here.

CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_envcrn_deleted_is_not_null ON sdxcluster (accountid, envCrn, deleted) WHERE deleted IS NOT NULL;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_crn_deleted_is_not_null ON sdxcluster (accountid, crn, deleted) WHERE deleted IS NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_envcrn_deleted_is_not_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_crn_deleted_is_not_null;