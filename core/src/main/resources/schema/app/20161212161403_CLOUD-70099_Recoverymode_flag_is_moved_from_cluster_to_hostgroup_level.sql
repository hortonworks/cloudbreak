-- // CLOUD-70099-Recoverymode flag is moved from cluster to hostgroup level
-- Migration SQL that makes the change goes here.

ALTER TABLE hostgroup ADD COLUMN recoverymode varchar(255) DEFAULT 'MANUAL';

UPDATE hostgroup SET recoverymode = cluster.recoverymode FROM cluster WHERE cluster.id = hostgroup.cluster_id;

ALTER TABLE cluster DROP COLUMN recoverymode;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster ADD COLUMN recoverymode varchar(255) DEFAULT 'MANUAL';

UPDATE cluster SET recoverymode = hostgroup.recoverymode FROM hostgroup WHERE cluster.id = hostgroup.cluster_id;

ALTER TABLE hostgroup DROP COLUMN recoverymode;
