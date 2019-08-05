-- // CDPD-1220 eliminate Datamart HA based cluster templates
-- Migration SQL that makes the change goes here.

UPDATE clustertemplate
SET status = 'DEFAULT_DELETED'
WHERE type = 'DATAMART_HA' AND name='CDP 1.0 - Data Mart HA template';


-- //@UNDO
-- SQL to undo the change goes here.

UPDATE clustertemplate
SET status = 'DEFAULT'
WHERE type = 'DATAMART_HA' AND name='CDP 1.0 - Data Mart HA template';