-- // CB-29760 rename hybrid blueprints and remove oozie
-- Migration SQL that makes the change goes here.

UPDATE blueprint SET name = '7.3.1 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive' WHERE status = 'DEFAULT' AND name = '7.3.1 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive, Apache Oozie';
UPDATE blueprint SET name = '7.3.2 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive' WHERE status = 'DEFAULT' AND name = '7.3.2 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive, Apache Oozie';


-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET name = '7.3.1 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive, Apache Oozie' WHERE status = 'DEFAULT' AND name = '7.3.1 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive';
UPDATE blueprint SET name = '7.3.2 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive, Apache Oozie' WHERE status = 'DEFAULT' AND name = '7.3.2 - Hybrid Data Engineering: HA: Apache Spark3, Apache Hive';

