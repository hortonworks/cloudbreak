-- // CB-3203 remove CM 6.2 version from distrox
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT'  AND name = 'CDH 6.2 - Data Science: Apache Spark, Apache Hive';
UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT'  AND name = 'CDH 6.2 - Data Lake: Hive Metastore';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED'  AND name = 'CDH 6.2 - Data Science: Apache Spark, Apache Hive';
UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED'  AND name = 'CDH 6.2 - Data Lake: Hive Metastore';
