-- // CB-747 remove HDP and HDF related blueprint and cluster templates
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND stacktype LIKE 'HDP';
UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND stacktype LIKE 'HDF';
UPDATE clustertemplate SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND stacktype LIKE 'HDP';
UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND stacktype LIKE 'HDF';
UPDATE clustertemplate SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED';
