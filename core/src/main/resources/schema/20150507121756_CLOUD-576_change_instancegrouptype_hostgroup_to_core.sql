-- // CLOUD-576_change_instancegrouptype_hostgroup_to_core
-- Migration SQL that makes the change goes here.

update instancegroup set instancegrouptype='CORE' where instancegrouptype='HOSTGROUP';

-- //@UNDO
-- SQL to undo the change goes here.


