-- // CLOUD-57310_update_hbase_ports_to_new_versions
-- Migration SQL that makes the change goes here.

UPDATE securitygroup SET description = REPLACE(REPLACE(description, ' 60010 (HBase Master Web)', ' 16010 (HBase Master Web)'), ' 60030 (HBase Region Server Info)', ' 16030 (HBase Region Server Info)') WHERE name='all-services-port' AND status='DEFAULT';
UPDATE securityrule SET ports = REPLACE(REPLACE(ports, ',60010,', ',16010,'), ',60030,', ',16030,') WHERE securitygroup_id IN (SELECT id FROM securitygroup WHERE name='all-services-port' AND status='DEFAULT');


-- //@UNDO
-- SQL to undo the change goes here.

UPDATE securitygroup SET description = REPLACE(REPLACE(description, ' 16010 (HBase Master Web)', ' 60010 (HBase Master Web)'), ' 16030 (HBase Region Server Info)', ' 60030 (HBase Region Server Info)') WHERE name='all-services-port' AND status='DEFAULT';
UPDATE securityrule SET ports = REPLACE(REPLACE(ports, ',16010,', ',60010,'), ',16030,', ',60030,') WHERE securitygroup_id IN (SELECT id FROM securitygroup WHERE name='all-services-port' AND status='DEFAULT');

