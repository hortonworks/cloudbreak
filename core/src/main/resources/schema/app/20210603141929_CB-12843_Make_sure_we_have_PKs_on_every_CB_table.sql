-- // CB-12843 Make sure we have PKs on every CB table
-- Migration SQL that makes the change goes here.

alter table volumetemplate drop constraint if exists volumetemplate_pkey;
alter table volumetemplate add primary key (id);

alter table userprofile drop constraint if exists userprofile_pkey;
alter table userprofile add primary key (id);

alter table securitygroup_securitygroupids drop constraint if exists securitygroup_securitygroupids_pkey;
alter table securitygroup_securitygroupids add primary key (securitygroup_id, securitygroupid_value);

alter table user_workspace_permissions_bkp drop constraint if exists user_workspace_permissions_bkp_pkey;
alter table user_workspace_permissions_bkp add primary key (id);

alter table topology_records drop constraint if exists topology_records_pkey;
alter table topology_records add primary key (topology_id, hypervisor, rack);

-- //@UNDO
-- SQL to undo the change goes here.

alter table volumetemplate drop constraint if exists volumetemplate_pkey;
alter table userprofile drop constraint if exists userprofile_pkey;
alter table securitygroup_securitygroupids drop constraint if exists securitygroup_securitygroupids_pkey;
alter table user_workspace_permissions_bkp drop constraint if exists user_workspace_permissions_bkp_pkey;
alter table topology_records drop constraint if exists topology_records_pkey;