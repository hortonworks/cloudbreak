-- // CB-4600 Rename template
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET name=CONCAT(name, ' deprecated'),status='DEFAULT_DELETED' WHERE status='DEFAULT';

DELETE FROM clustertemplate WHERE status='DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.
