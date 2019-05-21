-- // Update DE blueprint name
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT'  AND name = 'CDP 1.0 - Data Engineering: Apache Spark, Apache Livy, Apache Zeppelin, Hue';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED'  AND name = 'CDP 1.0 - Data Engineering: Apache Spark, Apache Livy, Apache Zeppelin, Hue';

-- No need for reverting this
