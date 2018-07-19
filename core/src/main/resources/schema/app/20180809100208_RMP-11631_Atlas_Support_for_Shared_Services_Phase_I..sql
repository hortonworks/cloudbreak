-- // RMP-11631 Atlas Support for Shared Services Phase I.
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET name='Data Lake: Apache Ranger, Apache Atlas, Apache Hive Metastore' WHERE name='Data Lake: Apache Ranger, Apache Hive Metastore';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET name='Data Lake: Apache Ranger, Apache Hive Metastore' WHERE name='Data Lake: Apache Ranger, Apache Atlas, Apache Hive Metastore';
