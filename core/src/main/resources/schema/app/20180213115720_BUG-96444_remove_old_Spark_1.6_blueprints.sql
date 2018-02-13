-- // BUG-96444 remove old Spark 1.6 blueprints
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE '%Apache Spark 1.6%';
UPDATE blueprint SET name = 'BI: Druid (Technical Preview)' WHERE status = 'DEFAULT' AND name = 'BI: Druid 0.9.2 (Technical Preview)';
UPDATE blueprint SET name = 'Data Science: Apache Spark 2, Apache Zeppelin' WHERE status = 'DEFAULT' AND name = 'Data Science: Apache Spark 2.1, Apache Zeppelin 0.7.0';
UPDATE blueprint SET name = 'EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin' WHERE status = 'DEFAULT' AND name = 'EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0';
UPDATE blueprint SET name = 'EDW-ETL: Apache Hive, Apache Spark 2' WHERE status = 'DEFAULT' AND name = 'EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.1';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE '%Apache Spark 1.6%';

-- no other undo since it does not make sense to write it back to the invalid names
