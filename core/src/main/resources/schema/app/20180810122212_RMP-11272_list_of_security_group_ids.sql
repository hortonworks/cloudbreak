-- // RMP-11272 list of security group ids
-- Migration SQL that makes the change goes here.

--- create the table
CREATE TABLE securitygroup_securitygroupids (
    securitygroup_id bigint NOT NULL,
    securitygroupid_value text
);
--- transfer the sceurity group id into the new table
INSERT INTO securitygroup_securitygroupids (securitygroup_id, securitygroupid_value)
SELECT id, securitygroupid FROM securitygroup
WHERE securitygroupid IS NOT NULL;
--- we don't remove the old column, for possibility of restoration of data

-- //@UNDO
-- SQL to undo the change goes here.

drop TABLE securitygroup_securitygroupids;
