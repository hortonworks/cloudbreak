-- // CB-9 SDX Management can post a valid cluster request to Cloudbreak
-- Migration SQL that makes the change goes here.

CREATE TABLE sdxcluster (

    id bigserial NOT NULL,
    accountid character varying(255) NOT NULL,
    stackid bigint,
    status character varying(255) NOT NULL,
    clustername character varying(255) NOT NULL,
    initiatorUserCrn character varying(255) NOT NULL,
    envname character varying(255) NOT NULL,
    accessCidr character varying(255) NOT NULL,
    clusterShape character varying(255) NOT NULL,
    tags character varying(255) NOT NULL,
    deleted bigint,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX unq_index_sdxcluster_clustername_accountid_deleted_is_null ON sdxcluster (clustername, accountid) WHERE deleted IS NULL;
CREATE UNIQUE INDEX unq_index_sdxcluster_clustername_accountid_deleted_is_not_null ON sdxcluster (clustername, accountid, deleted) WHERE deleted IS NOT NULL;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_envname_deleted_is_null ON sdxcluster (accountid, envname) WHERE deleted IS NULL;
CREATE UNIQUE INDEX unq_index_sdxcluster_accountid_envname_deleted_is_not_null ON sdxcluster (accountid, envname, deleted) WHERE deleted IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sdxcluster_accountid_envname_deleted ON sdxcluster USING btree (accountid, envname, deleted);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_sdxcluster_accountid_envname_deleted;
DROP INDEX IF EXISTS unq_index_sdxcluster_clustername_accountid_deleted_is_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_clustername_accountid_deleted_is_not_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_envname_deleted_is_null;
DROP INDEX IF EXISTS unq_index_sdxcluster_accountid_envname_deleted_is_not_null;
DROP TABLE IF EXISTS sdxcluster;