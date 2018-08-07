-- // BUG-104098 Shared Service follow ups
-- Migration SQL that makes the change goes here.

update blueprint set name='Data Lake: Apache Ranger, Apache Hive Metastore' where name='Enterprise Services';

-- //@UNDO
-- SQL to undo the change goes here.

update blueprint set name='Enterprise Services' where name='Data Lake: Apache Ranger, Apache Hive Metastore';
