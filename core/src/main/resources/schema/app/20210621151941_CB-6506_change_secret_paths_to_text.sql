-- // CB-6506 Change secret paths to text
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster
    ALTER COLUMN username TYPE TEXT,
    ALTER COLUMN password TYPE TEXT,
    ALTER COLUMN cloudbreakambariuser TYPE TEXT,
    ALTER COLUMN cloudbreakclustermanageruser TYPE TEXT,
    ALTER COLUMN cloudbreakambaripassword TYPE TEXT,
    ALTER COLUMN cloudbreakclustermanagerpassword TYPE TEXT,
    ALTER COLUMN dpambariuser TYPE TEXT,
    ALTER COLUMN dpclustermanageruser TYPE TEXT,
    ALTER COLUMN dpambaripassword TYPE TEXT,
    ALTER COLUMN dpclustermanagerpassword TYPE TEXT,
    ALTER COLUMN databuscredential TYPE TEXT;

ALTER TABLE gateway
    ALTER COLUMN knoxmastersecret TYPE TEXT;

ALTER TABLE idbroker
    ALTER COLUMN mastersecret TYPE TEXT;

ALTER TABLE rdsconfig
    ALTER COLUMN connectionusername TYPE TEXT,
    ALTER COLUMN connectionpassword TYPE TEXT;

ALTER TABLE saltsecurityconfig
    ALTER COLUMN saltpassword TYPE TEXT,
    ALTER COLUMN saltsignprivatekey TYPE TEXT,
    ALTER COLUMN saltbootpassword TYPE TEXT,
    ALTER COLUMN saltbootsignprivatekey TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

-- Left blank, can't rollback if longer data was inserted therefore we leave columns as text
