-- // CB-3372 remove CDP 1.0 blueprints and templates
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE 'CDP 1.0%';

UPDATE clustertemplate SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE 'CDP 1.0%';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE 'CDP 1.0%';

UPDATE clustertemplate SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE 'CDP 1.0%';
