-- // CB-16263 rename opdb templates
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET name='7.2.13 - Operational Database: Apache HBase, Phoenix (deprecated)' WHERE name='7.2.13 - Operational Database: Apache HBase, Phoenix' and status='DEFAULT_DELETED';
UPDATE blueprint SET name='7.2.14 - Operational Database: Apache HBase, Phoenix (deprecated)' WHERE name='7.2.14 - Operational Database: Apache HBase, Phoenix' and status='DEFAULT_DELETED';

-- //@UNDO
-- SQL to undo the change goes here.

