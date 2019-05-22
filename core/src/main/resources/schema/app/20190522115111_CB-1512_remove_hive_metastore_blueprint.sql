-- // CB-1512 remove hive metastore blueprint
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND stackname = 'datalake'  AND name <> 'CDP 1.0 - SDX: Apache Hive Metastore, Apache Ranger, Apache Atlas';

-- //@UNDO
-- SQL to undo the change goes here.


-- No need for reverting this


