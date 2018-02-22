-- // BUG-96715 remove default 2.5 blueprints
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE
  status = 'DEFAULT' AND name='EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.0' OR name='EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.6.0';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE
  status = 'DEFAULT_DELETED' AND name='EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.0' OR name='EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.6.0';
