-- // CB-3619 Hide the DE HA blueprint template from the CDP prod UI
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = 'CDP 1.1 - Data Engineering HA: Apache Spark, Apache Livy, Apache Zeppelin';

UPDATE clustertemplate SET status='DEFAULT_DELETED' where name='CDP 1.1 - Data Engineering HA template';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = 'CDP 1.1 - Data Engineering HA: Apache Spark, Apache Livy, Apache Zeppelin';

UPDATE clustertemplate SET status='DEFAULT' where name='CDP 1.1 - Data Engineering HA template';