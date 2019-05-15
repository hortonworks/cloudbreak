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
    PRIMARY KEY (id),
    CONSTRAINT unq_sdxcluster_accountid_envname UNIQUE(accountid, envname),
    CONSTRAINT unq_sdxcluster_accountid_envname_clustername UNIQUE(accountid, envname, clustername)
);

CREATE INDEX IF NOT EXISTS idx_sdxcluster_accountid_envname ON sdxcluster USING btree (accountid, envname);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_sdxcluster_accountid_envname;
DROP TABLE IF EXISTS sdxcluster;