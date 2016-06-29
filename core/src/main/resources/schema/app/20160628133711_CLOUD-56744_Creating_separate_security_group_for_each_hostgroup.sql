-- // CLOUD-56744 Creating separate security group for each hostgroup
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD COLUMN securitygroup_id bigint;

ALTER TABLE ONLY instancegroup ADD CONSTRAINT fk_securitygroupidininstancegroup FOREIGN KEY (securitygroup_id) REFERENCES securitygroup(id);

UPDATE instancegroup ig SET securitygroup_id=s.securitygroup_id FROM stack s WHERE ig.stack_id = s.id;

ALTER TABLE ONLY stack DROP CONSTRAINT IF EXISTS fk_securitygroupidinstack;

ALTER TABLE stack DROP COLUMN securitygroup_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY instancegroup DROP CONSTRAINT IF EXISTS fk_securitygroupidininstancegroup;

ALTER TABLE instancegroup DROP COLUMN securitygroup_id;

ALTER TABLE stack ADD COLUMN securitygroup_id bigint;

ALTER TABLE ONLY stack ADD CONSTRAINT fk_securitygroupidinstack FOREIGN KEY (securitygroup_id) REFERENCES securitygroup(id);




