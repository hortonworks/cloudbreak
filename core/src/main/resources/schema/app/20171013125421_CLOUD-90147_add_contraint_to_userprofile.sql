-- // CLOUD-90147 add contraint to userprofile
-- Migration SQL that makes the change goes here.

ALTER TABLE userprofile ADD CONSTRAINT uk_userprofile_account_name UNIQUE (account, owner);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE userprofile DROP CONSTRAINT uk_userprofile_account_name;
