-- // CB-1647 rename environment to environmentcrn
-- Migration SQL that makes the change goes here.

ALTER TABLE stack RENAME COLUMN environment TO environmentcrn;
ALTER TABLE kerberosconfig RENAME COLUMN environmentid TO environmentcrn;
ALTER TABLE ldapconfig RENAME COLUMN environmentid TO environmentcrn;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack RENAME COLUMN environmentcrn TO environment;
ALTER TABLE kerberosconfig RENAME COLUMN environmentcrn TO environmentid;
ALTER TABLE ldapconfig RENAME COLUMN environmentcrn TO environmentid;