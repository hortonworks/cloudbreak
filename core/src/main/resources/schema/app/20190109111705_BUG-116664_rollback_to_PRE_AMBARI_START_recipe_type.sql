-- // BUG-116664 rollback to PRE_AMBARI_START recipe type
-- Migration SQL that makes the change goes here.

UPDATE recipe SET recipetype='PRE_AMBARI_START' WHERE recipetype='PRE_CLUSTER_MANAGER_START';
UPDATE recipe SET recipetype='POST_AMBARI_START' WHERE recipetype='POST_CLUSTER_MANAGER_START';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE recipe SET recipetype='PRE_CLUSTER_MANAGER_START' WHERE recipetype='PRE_AMBARI_START';
UPDATE recipe SET recipetype='POST_CLUSTER_MANAGER_START' WHERE recipetype='POST_AMBARI_START';


