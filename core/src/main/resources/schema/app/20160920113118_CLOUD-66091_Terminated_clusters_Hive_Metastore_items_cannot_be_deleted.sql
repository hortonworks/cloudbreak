-- // CLOUD-66091 Terminated clusters Hive Metastore items cannot be deleted
-- Migration SQL that makes the change goes here.

update cluster set rdsconfig_id=null where status='DELETE_COMPLETED';

-- //@UNDO
-- SQL to undo the change goes here.


