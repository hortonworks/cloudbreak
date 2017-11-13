-- // CLOUD-91647 refactor the recipe types
-- Migration SQL that makes the change goes here.

UPDATE recipe SET recipetype='POST_AMBARI_START' WHERE recipetype='PRE';
UPDATE recipe SET recipetype='POST_CLUSTER_INSTALL' WHERE recipetype='POST';
DELETE FROM recipe WHERE recipetype='LEGACY' OR recipetype='MIGRATED';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE recipe SET recipetype='PRE' WHERE recipetype='POST_AMBARI_START';
UPDATE recipe SET recipetype='POST' WHERE recipetype='POST_CLUSTER_INSTALL';
