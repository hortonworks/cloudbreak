-- // CB-99 CDH clusterdefinition name fix
-- Migration SQL that makes the change goes here.

UPDATE clusterdefinition SET name = 'CDH 6.1 - Data Science: Apache Spark, Apache Hive, Impala' WHERE status = 'DEFAULT' AND name = 'CDH 6.1 - Data Science: Apache Spark, Apach Hive, Impala';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE clusterdefinition SET name = 'CDH 6.1 - Data Science: Apache Spark, Apach Hive, Impala' WHERE status = 'DEFAULT' AND name = 'CDH 6.1 - Data Science: Apache Spark, Apache Hive, Impala';
