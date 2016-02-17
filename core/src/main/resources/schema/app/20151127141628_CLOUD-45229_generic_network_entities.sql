-- // generic 
-- Migration SQL that makes the change goes here.

ALTER TABLE network ADD COLUMN attributes TEXT;
ALTER TABLE network ADD COLUMN cloudplatform VARCHAR(255);

UPDATE network SET attributes = json_object(array['vpcId', vpcid, 'internetGatewayId', internetgatewayid]) WHERE dtype = 'AwsNetwork' AND vpcid IS NOT NULL;
UPDATE network SET attributes = json_object(array['addressPrefixCIDR', addressprefixcidr]) WHERE dtype = 'AzureNetwork';
UPDATE network SET attributes = json_object(array['publicNetId', publicnetid ]) WHERE dtype = 'OpenStackNetwork';

UPDATE network SET cloudplatform = 'AWS' WHERE dtype = 'AwsNetwork';
UPDATE network SET cloudplatform = 'AZURE' WHERE dtype = 'AzureNetwork';
UPDATE network SET cloudplatform = 'GCP' WHERE dtype = 'GcpNetwork';
UPDATE network SET cloudplatform = 'OPENSTACK' WHERE dtype = 'OpenStackNetwork';

ALTER TABLE network ALTER COLUMN cloudplatform SET NOT NULL;

ALTER TABLE network DROP COLUMN vpcid;
ALTER TABLE network DROP COLUMN internetgatewayid;
ALTER TABLE network DROP COLUMN addressprefixcidr;
ALTER TABLE network DROP COLUMN publicnetid;
ALTER TABLE network DROP COLUMN dtype;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE network ADD COLUMN vpcid VARCHAR(255);
ALTER TABLE network ADD COLUMN internetgatewayid VARCHAR(255);
ALTER TABLE network ADD COLUMN addressprefixcidr VARCHAR(255);
ALTER TABLE network ADD COLUMN publicnetid VARCHAR(255);
ALTER TABLE network ADD COLUMN dtype varchar(50);

UPDATE network SET dtype = 'AwsNetwork' WHERE cloudplatform = 'AWS';
UPDATE network SET dtype = 'AzureNetwork' WHERE cloudplatform = 'AZURE';
UPDATE network SET dtype = 'GcpNetwork' WHERE cloudplatform = 'GCP';
UPDATE network SET dtype = 'OpenStackNetwork' WHERE cloudplatform = 'OPENSTACK';

ALTER TABLE network ALTER COLUMN attributes SET DATA TYPE jsonb USING attributes::jsonb;

UPDATE network SET vpcid = attributes->> 'vpcId', internetgatewayid = attributes->> 'internetGatewayId' WHERE cloudplatform = 'AWS' AND (attributes->>'vpcId') IS NOT NULL AND (attributes->>'internetGatewayId') IS NOT NULL;
UPDATE network SET addressprefixcidr = attributes->> 'addressPrefixCIDR' WHERE cloudplatform = 'AZURE' AND (attributes->>'addressPrefixCIDR') IS NOT NULL;
UPDATE network SET publicnetid = attributes->> 'publicNetId' WHERE cloudplatform = 'OPENSTACK' AND (attributes->>'publicNetId') IS NOT NULL;

ALTER TABLE network DROP COLUMN attributes;
ALTER TABLE network DROP COLUMN cloudplatform;