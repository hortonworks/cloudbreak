-- // CLOUD-81698 cleanup LDAP domain
-- Migration SQL that makes the change goes here.


ALTER TABLE ldapConfig DROP COLUMN userSearchFilter;
ALTER TABLE ldapConfig DROP COLUMN groupSearchFilter;
ALTER TABLE ldapConfig DROP COLUMN principalRegex;


ALTER TABLE ldapConfig RENAME COLUMN userSearchAttribute TO userNameAttribute;
ALTER TABLE ldapConfig RENAME COLUMN groupIdAttribute TO groupNameAttribute;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapConfig ADD COLUMN userSearchFilter text;
ALTER TABLE ldapConfig ADD COLUMN groupSearchFilter text;
ALTER TABLE ldapConfig ADD COLUMN principalRegex text;

ALTER TABLE ldapConfig RENAME COLUMN userNameAttribute TO userSearchAttribute;
ALTER TABLE ldapConfig RENAME COLUMN groupNameAttribute TO groupIdAttribute;