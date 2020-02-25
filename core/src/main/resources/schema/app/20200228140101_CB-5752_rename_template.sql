-- // CB-5752 OpDB 7.1.0 cluster definitions out of sync for AWS and Azure
-- Migration SQL that makes the change goes here.

UPDATE clustertemplate SET name = '7.1.0 - Operational Database with SQL for AWS' WHERE status = 'DEFAULT' AND name = '7.1.0 - Operational Database with Phoenix for AWS';
UPDATE clustertemplate SET name = '7.1.0 - Operational Database with SQL for Azure' WHERE status = 'DEFAULT' AND name = '7.1.0 - Operational Database for Azure';
UPDATE blueprint SET status='DEFAULT_DELETED' WHERE name = '7.1.0 - Operational Database: Apache HBase';
UPDATE blueprint SET status='DEFAULT_DELETED' WHERE name = 'CDP 1.2 - Operational Database: Apache HBase, Phoenix';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE clustertemplate SET name = '7.1.0 - Operational Database with Phoenix for AWS' WHERE status = 'DEFAULT' AND name = '7.1.0 - Operational Database with SQL for AWS';
UPDATE clustertemplate SET name = '7.1.0 - Operational Database for Azure' WHERE status = 'DEFAULT' AND name = '7.1.0 - Operational Database with SQL for Azure';
UPDATE blueprint SET status='DEFAULT' WHERE name = '7.1.0 - Operational Database: Apache HBase';
UPDATE blueprint SET status='DEFAULT' WHERE name = 'CDP 1.2 - Operational Database: Apache HBase, Phoenix';
