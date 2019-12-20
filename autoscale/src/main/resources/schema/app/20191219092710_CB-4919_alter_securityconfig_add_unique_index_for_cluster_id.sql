-- // CB-4919 alter securityconfig add unique index for cluster_id
-- Migration SQL that makes the change goes here.

CREATE UNIQUE INDEX IF NOT EXISTS securityconfig_cluster_id_uindex
    ON securityconfig (cluster_id);

-- //@UNDO
-- SQL to undo the change goes here.


DROP INDEX IF EXISTS securityconfig_cluster_id_uindex;