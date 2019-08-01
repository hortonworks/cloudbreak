-- // CDPD-1220 Add initial working DataMart template
-- Migration SQL that makes the change goes here.

UPDATE blueprint
SET status = 'DEFAULT_DELETED'
WHERE status = 'DEFAULT'
AND (name='CDP 1.0 - Data Mart HA: Apache Impala, Hue');

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint
SET status = 'DEFAULT'
WHERE status = 'DEFAULT_DELETED'
AND (name='CDP 1.0 - Data Mart HA: Apache Impala, Hue');
