-- // CLOUD-45229 generic template entities
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN attributes TEXT;
ALTER TABLE template ADD COLUMN cloudplatform VARCHAR(255);

UPDATE template SET attributes = json_build_object('spotPrice', spotprice, 'sshLocation', sshlocation, 'encrypted', encrypted::boolean) WHERE dtype = 'AwsTemplate';
UPDATE template SET volumetype = 'HDD' WHERE dtype = 'AzureTemplate';
UPDATE template SET volumetype = 'HDD' WHERE dtype = 'OpenStackTemplate';
UPDATE template SET volumetype = gcprawdisktype WHERE dtype = 'GcpTemplate';

UPDATE template SET cloudplatform = 'AWS' WHERE dtype = 'AwsTemplate';
UPDATE template SET cloudplatform = 'AZURE' WHERE dtype = 'AzureTemplate';
UPDATE template SET cloudplatform = 'GCP' WHERE dtype = 'GcpTemplate';
UPDATE template SET cloudplatform = 'OPENSTACK' WHERE dtype = 'OpenStackTemplate';

ALTER TABLE template ALTER COLUMN cloudplatform SET NOT NULL;

ALTER TABLE template DROP COLUMN gcprawdisktype;
ALTER TABLE template DROP COLUMN dtype;
ALTER TABLE template DROP COLUMN encrypted;
ALTER TABLE template DROP COLUMN spotprice;
ALTER TABLE template DROP COLUMN sshlocation;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template ADD COLUMN dtype VARCHAR(50);
ALTER TABLE template ADD COLUMN gcprawdisktype VARCHAR(255);
ALTER TABLE template ADD COLUMN encrypted VARCHAR(255);
ALTER TABLE template ADD COLUMN spotprice DOUBLE PRECISION;
ALTER TABLE template ADD COLUMN sshlocation VARCHAR(50);

UPDATE template SET dtype = 'AwsTemplate' WHERE cloudplatform = 'AWS';
UPDATE template SET dtype = 'AzureTemplate' WHERE cloudplatform = 'AZURE';
UPDATE template SET dtype = 'GcpTemplate' WHERE cloudplatform = 'GCP';
UPDATE template SET dtype = 'OpenStackTemplate' WHERE cloudplatform = 'OPENSTACK';

ALTER TABLE template ALTER COLUMN attributes SET DATA TYPE jsonb USING attributes::jsonb;

UPDATE template SET sshlocation = attributes->> 'sshLocation', encrypted = UPPER(attributes->> 'encrypted'), spotprice = CAST(attributes->> 'spotPrice' AS DOUBLE PRECISION) WHERE cloudplatform = 'AWS';
UPDATE template SET gcprawdisktype = volumetype WHERE dtype = 'GcpTemplate';

ALTER TABLE template DROP COLUMN attributes;
ALTER TABLE template DROP COLUMN cloudplatform;