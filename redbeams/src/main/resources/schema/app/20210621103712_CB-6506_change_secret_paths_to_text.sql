-- // CB-6506 Change secret paths to text
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseconfig
    ALTER COLUMN connectionusername TYPE TEXT,
    ALTER COLUMN connectionpassword TYPE TEXT;

ALTER TABLE databaseserver
    ALTER COLUMN rootusername TYPE TEXT,
    ALTER COLUMN rootpassword TYPE TEXT;

ALTER TABLE databaseserverconfig
    ALTER COLUMN connectionusername TYPE TEXT,
    ALTER COLUMN connectionpassword TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

-- Left blank, can't rollback if longer data was inserted therefore we leave columns as text
