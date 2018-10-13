-- // RMP-12337 - add tenant to UserProfile
-- Migration SQL that makes the change goes here.

ALTER TABLE userprofile DROP CONSTRAINT IF EXISTS uk_userprofile_account_name;
ALTER TABLE userprofile
 ADD CONSTRAINT uk_userprofile_userid UNIQUE (user_id);

ALTER TABLE users DROP CONSTRAINT users_userid_is_unique;
ALTER TABLE users
 ADD CONSTRAINT users_userid_tenantid_is_unique UNIQUE (userid, tenant_id);

ALTER TABLE users DROP CONSTRAINT users_email_key;

drop index users_userid_idx;

alter table blueprint alter column account drop not null;
alter table blueprint alter column owner drop not null;

alter table cloudbreakusage alter column account drop not null;
alter table cloudbreakusage alter column owner drop not null;

alter table constrainttemplate alter column account drop not null;
alter table constrainttemplate alter column owner drop not null;

alter table credential alter column account drop not null;
alter table credential alter column owner drop not null;

alter table filesystem alter column account drop not null;
alter table filesystem alter column owner drop not null;

alter table flexsubscription alter column account drop not null;
alter table flexsubscription alter column owner drop not null;

alter table imagecatalog alter column account drop not null;
alter table imagecatalog alter column owner drop not null;

alter table ldapconfig alter column account drop not null;
alter table ldapconfig alter column owner drop not null;

alter table managementpack alter column account drop not null;
alter table managementpack alter column owner drop not null;

alter table network alter column account drop not null;
alter table network alter column owner drop not null;

alter table proxyconfig alter column account drop not null;
alter table proxyconfig alter column owner drop not null;

alter table rdsconfig alter column account drop not null;
alter table rdsconfig alter column owner drop not null;

alter table recipe alter column account drop not null;
alter table recipe alter column owner drop not null;

alter table securitygroup alter column account drop not null;
alter table securitygroup alter column owner drop not null;

alter table smartsensesubscription alter column account drop not null;
alter table smartsensesubscription alter column owner drop not null;

alter table structuredevent alter column account drop not null;
alter table structuredevent alter column userid drop not null;

alter table template alter column account drop not null;
alter table template alter column owner drop not null;

alter table userprofile alter column account drop not null;
alter table userprofile alter column owner drop not null;

alter table stack alter column account drop not null;
alter table stack alter column owner drop not null;

alter table cluster alter column account drop not null;
alter table cluster alter column owner drop not null;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE userprofile DROP CONSTRAINT IF EXISTS uk_userprofile_userid;
ALTER TABLE userprofile
 ADD CONSTRAINT uk_userprofile_account_name UNIQUE (account, owner);

ALTER TABLE users DROP CONSTRAINT users_userid_tenantid_is_unique;
ALTER TABLE users
 ADD CONSTRAINT users_userid_is_unique UNIQUE (userid);

create unique index users_userid_idx
  on users (userid);
  
alter table blueprint alter column account set not null;
alter table blueprint alter column owner set not null;

alter table cloudbreakusage alter column account set not null;
alter table cloudbreakusage alter column owner set not null;

alter table constrainttemplate alter column account set not null;
alter table constrainttemplate alter column owner set not null;

alter table credential alter column account set not null;
alter table credential alter column owner set not null;

alter table filesystem alter column account set not null;
alter table filesystem alter column owner set not null;

alter table flexsubscription alter column account set not null;
alter table flexsubscription alter column owner set not null;

alter table imagecatalog alter column account set not null;
alter table imagecatalog alter column owner set not null;

alter table ldapconfig alter column account set not null;
alter table ldapconfig alter column owner set not null;

alter table managementpack alter column account set not null;
alter table managementpack alter column owner set not null;

alter table network alter column account set not null;
alter table network alter column owner set not null;

alter table proxyconfig alter column account set not null;
alter table proxyconfig alter column owner set not null;

alter table rdsconfig alter column account set not null;
alter table rdsconfig alter column owner set not null;

alter table recipe alter column account set not null;
alter table recipe alter column owner set not null;

alter table securitygroup alter column account set not null;
alter table securitygroup alter column owner set not null;

alter table smartsensesubscription alter column account set not null;
alter table smartsensesubscription alter column owner set not null;

alter table structuredevent alter column account set not null;
alter table structuredevent alter column userid set not null;

alter table template alter column account set not null;
alter table template alter column owner set not null;

alter table userprofile alter column account set not null;
alter table userprofile alter column owner set not null;

alter table stack alter column account set not null;
alter table stack alter column owner set not null;

alter table cluster alter column account set not null;
alter table cluster alter column owner set not null;