-- // CB-11570 Add cluster display name in sdxcluster table
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS clusterDisplayName character varying(255);
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_crn_deleted_is_not_null;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_crn_deleted_is_not_null_detached_false ON sdxcluster (accountid, crn, deleted, detached) WHERE deleted IS NOT NULL AND detached IS FALSE;;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE sdxcluster DROP COLUMN clusterDisplayName;
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_crn_deleted_is_not_null_detached_false;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_crn_deleted_is_not_null ON sdxcluster (accountid, crn, deleted) WHERE deleted IS NOT NULL;
