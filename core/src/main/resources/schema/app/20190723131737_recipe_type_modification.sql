-- // recipe type modification
-- Migration SQL that makes the change goes here.

UPDATE recipe set recipeType='PRE_CLOUDERA_MANAGER_START' where recipetype='PRE_AMBARI_START';
UPDATE recipe set recipeType='POST_CLOUDERA_MANAGER_START' where recipetype='POST_AMBARI_START';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE recipe set recipeType='PRE_AMBARI_START' where recipeType='PRE_CLOUDERA_MANAGER_START';
UPDATE recipe set recipeType='POST_AMBARI_START' where recipeType='POST_CLOUDERA_MANAGER_START';