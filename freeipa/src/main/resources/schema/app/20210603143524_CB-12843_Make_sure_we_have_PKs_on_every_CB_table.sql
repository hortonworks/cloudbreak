-- // CB-12843 Make sure we have PKs on every CB table
-- Migration SQL that makes the change goes here.

alter table securitygroup_securitygroupids drop constraint if exists securitygroup_securitygroupids_pkey;
alter table securitygroup_securitygroupids add primary key (securitygroup_id, securitygroupid_value);

-- //@UNDO
-- SQL to undo the change goes here.

alter table securitygroup_securitygroupids drop constraint if exists securitygroup_securitygroupids_pkey;
