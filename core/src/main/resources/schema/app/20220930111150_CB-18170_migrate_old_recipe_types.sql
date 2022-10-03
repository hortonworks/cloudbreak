-- // CB-18170 migrate old recipe types
-- Migration SQL that makes the change goes here.

UPDATE recipe SET recipetype = 'PRE_SERVICE_DEPLOYMENT' WHERE recipetype = 'PRE_CLOUDERA_MANAGER_START';
UPDATE recipe SET recipetype = 'POST_SERVICE_DEPLOYMENT' WHERE recipetype = 'POST_CLUSTER_INSTALL';

-- //@UNDO
-- SQL to undo the change goes here.