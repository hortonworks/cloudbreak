-- // CB-1334 revert renaming Data Mart to Data Science
-- Migration SQL that makes the change goes here.

UPDATE blueprint
SET status = 'DEFAULT'
WHERE status = 'DEFAULT_DELETED'
AND name='CDP 1.0 - Data Science: Apache Spark, Apache Hive, Impala';

UPDATE blueprint
SET status = 'DEFAULT_DELETED'
WHERE status = 'DEFAULT'
AND name='CDP 1.0 - Data Mart: Apache Spark, Apache Hive, Impala, Hue';


-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint
SET status = 'DEFAULT_DELETED'
WHERE status = 'DEFAULT'
AND name='CDP 1.0 - Data Science: Apache Spark, Apache Hive, Impala';

UPDATE blueprint
SET status = 'DEFAULT'
WHERE status = 'DEFAULT_DELETED'
AND name='CDP 1.0 - Data Mart: Apache Spark, Apache Hive, Impala, Hue';
