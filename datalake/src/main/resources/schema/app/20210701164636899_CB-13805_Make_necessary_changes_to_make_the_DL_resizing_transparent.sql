-- // CB-18305: Make necessary changes to make the DL resizing transparent to experiences and UI
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_crn_deleted_is_not_null;
CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accountid_crn_deleted_is_not_null_detached_false ON sdxcluster (accountid, crn, deleted, detached) WHERE deleted IS NOT NULL AND detached IS FALSE;

DROP INDEX IF EXISTS unq_index_sdxcluster_clustername_accountid_deleted_is_not_null;
CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_name_actid_deleted_is_not_null_detached_false ON sdxcluster (accountid, clustername, deleted, detached) WHERE deleted IS NOT NULL AND detached IS FALSE;

DROP INDEX IF EXISTS unq_index_sdxcluster_clustername_accountid_deleted_is_null;

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS originalcrn varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS originalcrn;

DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_crn_deleted_is_not_null_detached_false;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_crn_deleted_is_not_null ON sdxcluster (accountid, crn, deleted) WHERE deleted IS NOT NULL;

DROP INDEX IF EXISTS unq_index_sdxcluster_name_actid_deleted_is_not_null_detached_false;
CREATE UNIQUE INDEX IF not exists unq_index_sdxcluster_clustername_accountid_deleted_is_not_null
	on sdxcluster (clustername, accountid, deleted)
	WHERE deleted IS NOT NULL;