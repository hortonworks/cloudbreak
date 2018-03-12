-- // BUG-98314 update RdsConfig from DEFAULT to DEFAULT_DELETED if not used
-- Migration SQL that makes the change goes here.

UPDATE rdsconfig SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND id NOT IN (SELECT cr.rdsconfigs_id FROM cluster_rdsconfig cr);


-- //@UNDO
-- SQL to undo the change goes here.


