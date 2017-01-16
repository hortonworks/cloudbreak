-- // CLOUD-72074 rename all services security group
-- Migration SQL that makes the change goes here.

UPDATE securitygroup SET name = 'UNSECURE-aws-all-services-open' WHERE name = 'default-aws-all-services-port';
UPDATE securitygroup SET name = 'UNSECURE-azure_rm-all-services-open' WHERE name = 'default-azure_rm-all-services-port';
UPDATE securitygroup SET name = 'UNSECURE-gcp-all-services-open' WHERE name = 'default-gcp-all-services-port';
UPDATE securitygroup SET name = 'UNSECURE-openstack-all-services-open' WHERE name = 'default-openstack-all-services-port';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE securitygroup SET name = 'default-aws-all-services-port' WHERE name = 'UNSECURE-aws-all-services-open';
UPDATE securitygroup SET name = 'default-azure_rm-all-services-port' WHERE name = 'UNSECURE-azure_rm-all-services-open';
UPDATE securitygroup SET name = 'default-gcp-all-services-port' WHERE name = 'UNSECURE-gcp-all-services-open';
UPDATE securitygroup SET name = 'default-openstack-all-services-port' WHERE name = 'UNSECURE-openstack-all-services-open';
