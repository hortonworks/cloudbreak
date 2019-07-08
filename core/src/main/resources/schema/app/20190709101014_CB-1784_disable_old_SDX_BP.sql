-- // CB-1988 Removing/Renaming old Blueprint for SDX
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE 'CDP 1.0 - SDX: Apache Hive Metastore, Apache Ranger, Apache Atlas';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE 'CDP 1.0 - SDX: Apache Hive Metastore, Apache Ranger, Apache Atlas';