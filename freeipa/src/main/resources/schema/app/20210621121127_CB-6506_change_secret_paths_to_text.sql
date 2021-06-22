-- // CB-6506 Change secret paths to text
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig
    ALTER COLUMN kerberospassword TYPE TEXT,
    ALTER COLUMN kerberosprincipal TYPE TEXT;

ALTER TABLE ldapconfig
    ALTER COLUMN binddn TYPE TEXT,
    ALTER COLUMN bindpassword TYPE TEXT;

ALTER TABLE stack
    ALTER COLUMN databuscredential TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

-- Left blank, can't rollback if longer data was inserted therefore we leave columns as text
