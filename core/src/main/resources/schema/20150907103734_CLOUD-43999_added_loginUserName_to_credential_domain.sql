-- // CLOUD-43999 added loginUserName to credential domain
-- Migration SQL that makes the change goes here.

ALTER TABLE credential ADD COLUMN loginusername text;
UPDATE credential SET loginusername='ec2-user' where dtype='OpenStackCredential';
UPDATE credential SET loginusername='ec2-user' where dtype='AwsCredential';
UPDATE credential SET loginusername='cloudbreak' where dtype='AzureCredential';
UPDATE credential SET loginusername='cloudbreak' where dtype='GcpCredential';
UPDATE credential SET loginusername='cloudbreak' where dtype='AzureRmCredential';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE credential DROP COLUMN loginusername;



