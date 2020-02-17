-- // CDPD-5755 Remove Hue HA template and definition
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = 'CDP 1.2 - Data Engineering Hue HA: Apache Spark, Apache Hive, Hue, Apache Oozie';
UPDATE clustertemplate SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = 'Data Engineering Hue HA for AWS';

UPDATE blueprint SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = '7.1.0 - Data Engineering Hue HA: Apache Spark, Apache Hive, Hue, Apache Oozie';
UPDATE clustertemplate SET status = 'DEFAULT_DELETED' WHERE status = 'DEFAULT' AND name = '7.1.0 - Data Engineering Hue HA for AWS';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = 'CDP 1.2 - Data Engineering Hue HA: Apache Spark, Apache Hive, Hue, Apache Oozie';
UPDATE clustertemplate SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = 'Data Engineering Hue HA for AWS';

UPDATE blueprint SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = '7.1.0 - Data Engineering Hue HA: Apache Spark, Apache Hive, Hue, Apache Oozie';
UPDATE clustertemplate SET status = 'DEFAULT' WHERE status = 'DEFAULT_DELETED' AND name = '7.1.0 - Data Engineering Hue HA for AWS';
