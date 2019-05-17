-- // CB-1334 add Hue to Data Science and Data Engineering templates
-- Migration SQL that makes the change goes here.

UPDATE blueprint
SET status = 'DEFAULT_DELETED'
WHERE status = 'DEFAULT'
AND (name='CDP 1.0 - Data Engineering: Apache Spark, Apache Livy, Apache Zeppelin' OR name='CDP 1.0 - Data Science: Apache Spark, Apache Hive, Impala');

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint
SET status = 'DEFAULT'
WHERE status = 'DEFAULT_DELETED'
AND (name='CDP 1.0 - Data Engineering: Apache Spark, Apache Livy, Apache Zeppelin' OR name='CDP 1.0 - Data Science: Apache Spark, Apache Hive, Impala');
