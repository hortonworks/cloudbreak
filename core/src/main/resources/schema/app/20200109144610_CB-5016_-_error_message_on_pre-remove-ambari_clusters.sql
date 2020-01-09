-- // CB-5016 - error message on pre-remove-ambari clusters
-- Migration SQL that makes the change goes here.
UPDATE stackstatus
SET detailedstackstatus = 'STARTING_CLUSTER_MANAGER_SERVICES'
WHERE detailedstackstatus = 'STARTING_AMBARI_SERVICES';


-- //@UNDO
-- SQL to undo the change goes here.
UPDATE stackstatus
SET detailedstackstatus = 'STARTING_AMBARI_SERVICES'
WHERE detailedstackstatus = 'STARTING_CLUSTER_MANAGER_SERVICES';


