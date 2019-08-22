-- // CB-3213 remove data science blueprints
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = 'CDP 1.0 - Data Science: Apache Spark, Apache Hive';
UPDATE clustertemplate SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = 'CDP 1.0 - Data Science template';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = 'CDP 1.0 - Data Science: Apache Spark, Apache Hive';
UPDATE clustertemplate SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = 'CDP 1.0 - Data Science template';
