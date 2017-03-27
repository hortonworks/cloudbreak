-- // instance metadata type for primary gateway
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN instanceMetadataType VARCHAR(255);
UPDATE instancemetadata SET instancemetadatatype='GATEWAY_PRIMARY' WHERE  instancegroup_id IN (SELECT id FROM instancegroup WHERE instancegrouptype='GATEWAY');
UPDATE instancemetadata SET instancemetadatatype='CORE' WHERE instancegroup_id NOT IN (SELECT id FROM instancegroup WHERE instancegrouptype='GATEWAY');

-- ALTER TABLE securityconfig ADD COLUMN instancemetadata_id bigint;
-- ALTER TABLE securityconfig ADD CONSTRAINT fk_securityconfig_instancemetadata_id FOREIGN KEY (instancemetadata_id) REFERENCES instancemetadata(id);
-- UPDATE securityconfig sc SET instancemetadata_id = (SELECT i.id FROM instancemetadata i LEFT JOIN instancegroup ig ON i.instancegroup_id=ig.id WHERE ig.instancegrouptype='GATEWAY' AND sc.stack_id=ig.stack_id);

ALTER TABLE instancemetadata ADD COLUMN servercert text;
UPDATE instancemetadata im SET servercert = (SELECT servercert FROM securityconfig sc INNER JOIN instancegroup ig ON ig.stack_id = sc.stack_id WHERE ig.id=im.instancegroup_id AND ig.instancegrouptype='GATEWAY');

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN instancemetadatatype;

ALTER TABLE instancemetadata DROP COLUMN servercert;

-- ALTER TABLE securityconfig DROP COLUMN servercert;