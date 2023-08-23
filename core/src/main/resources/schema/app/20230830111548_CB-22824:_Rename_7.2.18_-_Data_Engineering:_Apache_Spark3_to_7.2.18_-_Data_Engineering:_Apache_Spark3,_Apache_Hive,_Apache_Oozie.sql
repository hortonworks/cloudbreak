-- // CB-22824: Rename 7.2.18 - Data Engineering: Apache Spark3 to 7.2.18 - Data Engineering: Apache Spark3, Apache Hive, Apache Oozie
-- Migration SQL that makes the change goes here.

DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering HA for AWS' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering for AWS' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Real-time Data Mart for AWS' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Discovery and Exploration for AWS' AND status='DEFAULT';

DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering HA for Azure' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering for Azure' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Real-time Data Mart for Azure' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Discovery and Exploration for Azure' AND status='DEFAULT';

DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering HA for Google Cloud' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering for Google Cloud' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Real-time Data Mart for Google Cloud' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Discovery and Exploration for Google Cloud' AND status='DEFAULT';

DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering HA for YCloud' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Engineering for YCloud' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Real-time Data Mart for YCloud' AND status='DEFAULT';
DELETE FROM clustertemplate WHERE name='7.2.18 - Data Discovery and Exploration for YCloud' AND status='DEFAULT';

UPDATE blueprint SET status='DEFAULT_DELETED' WHERE name='7.2.18 - Data Engineering: Apache Spark, Apache Hive, Apache Oozie' AND status='DEFAULT';
UPDATE blueprint SET status='DEFAULT_DELETED' WHERE name='7.2.18 - Data Engineering: HA: Apache Spark, Apache Hive, Apache Oozie' AND status='DEFAULT';
UPDATE blueprint SET status='DEFAULT_DELETED' WHERE name='7.2.18 - Real-time Data Mart: Apache Impala, Hue, Apache Kudu, Apache Spark' AND status='DEFAULT';
UPDATE blueprint SET status='DEFAULT_DELETED' WHERE name='7.2.18 - Data Discovery and Exploration' AND status='DEFAULT';

UPDATE blueprint SET name='7.2.18 - Data Engineering: Apache Spark3, Apache Hive, Apache Oozie' WHERE name='7.2.18 - Data Engineering: Apache Spark3' AND status='DEFAULT';

-- //@UNDO
-- SQL to undo the change goes here.


