-- // CB-12871 Altering the table to detached column and indexs to include detached flag
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS idx_sdxcluster_accountid_envname_deleted;
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_envname_deleted_is_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_envname_deleted_is_not_null;

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS detached BOOLEAN NOT NULL DEFAULT FALSE;



CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accid_envname_deleted_null_detached_true ON sdxcluster (accountid, envname, detached) WHERE deleted IS NULL and detached is true;
CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxcluster_accid_envname_deleted_not_null_detached ON sdxcluster (accountid, envname, deleted, detached) WHERE deleted IS NOT NULL and detached is true;
CREATE INDEX IF NOT EXISTS idx_sdxcluster_accid_envname_deleted_detached ON sdxcluster USING btree (accountid, envname, deleted, detached);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS unq_index_sdxcluster_accid_envname_deleted_null_detached_true;
DROP INDEX IF EXISTS unq_index_sdxcluster_accid_envname_deleted_not_null_detached;
DROP INDEX IF EXISTS idx_sdxcluster_accid_envname_deleted_detached;

ALTER TABLE sdxcluster DROP COLUMN detached;

CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_envname_deleted_is_null ON sdxcluster (accountid, envname) WHERE deleted IS NULL;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_envname_deleted_is_not_null ON sdxcluster (accountid, envname, deleted) WHERE deleted IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sdxcluster_accountid_envname_deleted ON sdxcluster USING btree (accountid, envname, deleted);

