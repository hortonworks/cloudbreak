-- // Git issue 2064 added two columns securitygroupId and cloudplatform to securitygroup table.
-- Migration SQL that makes the change goes here.

ALTER TABLE securitygroup ADD COLUMN securitygroupid CHARACTER VARYING (255);
ALTER TABLE securitygroup ADD COLUMN cloudplatform CHARACTER VARYING (255);

UPDATE securitygroup SET name = 'default-aws-all-services-port' WHERE name = 'all-services-port';
UPDATE securitygroup SET name = 'default-aws-only-ssh-and-ssl' WHERE name = 'only-ssh-and-ssl';

UPDATE securitygroup SET cloudplatform='AWS' WHERE name = 'default-aws-all-services-port';
UPDATE securitygroup SET cloudplatform='AWS' WHERE name = 'default-aws-only-ssh-and-ssl';
UPDATE securitygroup SET cloudplatform='AZURE_RM' WHERE name = 'default-azure_rm-all-services-port';
UPDATE securitygroup SET cloudplatform='AZURE_RM' WHERE name = 'default-azure_rm-only-ssh-and-ssl';
UPDATE securitygroup SET cloudplatform='GCP' WHERE name = 'default-gcp-all-services-port';
UPDATE securitygroup SET cloudplatform='GCP' WHERE name = 'default-gcp-only-ssh-and-ssl';
UPDATE securitygroup SET cloudplatform='OPENSTACK' WHERE name = 'default-openstack-all-services-port';
UPDATE securitygroup SET cloudplatform='OPENSTACK' WHERE name = 'default-openstack-only-ssh-and-ssl';

UPDATE securitygroup SET cloudplatform='AWS' WHERE cloudplatform is NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securitygroup DROP COLUMN IF EXISTS securitygroupid;
ALTER TABLE securitygroup DROP COLUMN IF EXISTS cloudplatform;