-- // CB-4001 Eliminate CDP-1.1 related blueprints and cluster templates
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE 'CDP 1.1%';

UPDATE clustertemplate SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name LIKE 'CDP 1.1%';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE 'CDP 1.1%';

UPDATE clustertemplate SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name LIKE 'CDP 1.1%';
