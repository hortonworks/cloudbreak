-- // CLOUD-78236 support multiple gw nodes
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN instanceMetadataType VARCHAR(255);
UPDATE instancemetadata SET instancemetadatatype='GATEWAY_PRIMARY' WHERE  instancegroup_id IN (SELECT id FROM instancegroup WHERE instancegrouptype='GATEWAY');
UPDATE instancemetadata SET instancemetadatatype='CORE' WHERE instancegroup_id NOT IN (SELECT id FROM instancegroup WHERE instancegrouptype='GATEWAY');

ALTER TABLE instancemetadata ADD COLUMN servercert text;
UPDATE instancemetadata im SET servercert = (SELECT servercert FROM securityconfig sc INNER JOIN instancegroup ig ON ig.stack_id = sc.stack_id WHERE ig.id=im.instancegroup_id AND ig.instancegrouptype='GATEWAY');
ALTER TABLE securityconfig DROP COLUMN servercert;

ALTER TABLE securityconfig ADD COLUMN saltSignPublicKey text;
ALTER TABLE securityconfig ADD COLUMN saltSignPrivateKey text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig ADD COLUMN servercert text;
UPDATE securityconfig sc SET servercert = (SELECT servercert FROM instancemetadata im INNER JOIN instancegroup ig ON ig.id = im.instancegroup_id WHERE im.instancemetadatatype='GATEWAY_PRIMARY' AND sc.stack_id=ig.stack_id);
ALTER TABLE instancemetadata DROP COLUMN servercert;

ALTER TABLE securityconfig DROP COLUMN saltSignPublicKey;
ALTER TABLE securityconfig DROP COLUMN saltSignPrivateKey;

ALTER TABLE instancemetadata DROP COLUMN instancemetadatatype;
