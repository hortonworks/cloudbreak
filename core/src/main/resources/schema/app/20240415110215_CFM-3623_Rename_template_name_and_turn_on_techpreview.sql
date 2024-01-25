-- // CFM-3623 Rename template name and turn on techpreview
-- Migration SQL that makes the change goes here.

UPDATE clustertemplate SET featurestate = 'PREVIEW' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND (name like '7.2.18 - Flow Management - NiFi 2 - Light Duty%' OR name like '7.2.18 - Flow Management - NiFi 2 - Heavy Duty%') AND featurestate = 'RELEASED';
UPDATE clustertemplate SET name = '7.2.18 - Flow Management - NiFi 2 - Light Duty for AWS' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND name like '7.2.18 - Flow Management - NiFi 2 - Light Duty for AWS - PREVIEW';
UPDATE clustertemplate SET name = '7.2.18 - Flow Management - NiFi 2 - Heavy Duty for AWS' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND name like '7.2.18 - Flow Management - NiFi 2 - Heavy Duty for AWS - PREVIEW';
UPDATE clustertemplate SET name = '7.2.18 - Flow Management - NiFi 2 - Light Duty for Azure' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND name like '7.2.18 - Flow Management - NiFi 2 - Light Duty for Azure - PREVIEW';
UPDATE clustertemplate SET name = '7.2.18 - Flow Management - NiFi 2 - Heavy Duty for Azure' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND name like '7.2.18 - Flow Management - NiFi 2 - Heavy Duty for Azure - PREVIEW';
UPDATE clustertemplate SET name = '7.2.18 - Flow Management - NiFi 2 - Light Duty for Google Cloud' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND name like '7.2.18 - Flow Management - NiFi 2 - Light Duty for Google Cloud - PREVIEW';
UPDATE clustertemplate SET name = '7.2.18 - Flow Management - NiFi 2 - Heavy Duty for Google Cloud' WHERE type = 'FLOW_MANAGEMENT' AND clouderaruntimeversion = '7.2.18' AND name like '7.2.18 - Flow Management - NiFi 2 - Heavy Duty for Google Cloud - PREVIEW';


-- //@UNDO
-- SQL to undo the change goes here.
