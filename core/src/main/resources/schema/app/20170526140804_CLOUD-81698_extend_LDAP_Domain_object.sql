-- // CLOUD-81698 extend LDAP Domain object
-- Migration SQL that makes the change goes here.


ALTER TABLE ldapConfig ADD COLUMN directoryType VARCHAR(63);
ALTER TABLE ldapConfig ADD COLUMN userObjectClass text;
ALTER TABLE ldapConfig ADD COLUMN groupObjectClass text;
ALTER TABLE ldapConfig ADD COLUMN groupIdAttribute text;
ALTER TABLE ldapConfig ADD COLUMN groupMemberAttribute text;

UPDATE ldapConfig SET userObjectClass = 'person' WHERE userObjectClass IS NULL;
UPDATE ldapConfig SET groupObjectClass = 'groupOfNames' WHERE groupObjectClass IS NULL;
UPDATE ldapConfig SET groupIdAttribute = 'cn' WHERE groupIdAttribute IS NULL;
UPDATE ldapConfig SET groupMemberAttribute = 'member' WHERE groupMemberAttribute IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapConfig DROP COLUMN directoryType;
ALTER TABLE ldapConfig DROP COLUMN userObjectClass;
ALTER TABLE ldapConfig DROP COLUMN groupObjectClass;
ALTER TABLE ldapConfig DROP COLUMN groupIdAttribute;
ALTER TABLE ldapConfig DROP COLUMN groupMemberAttribute;
