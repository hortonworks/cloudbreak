-- // RMP-12837 introduce dp amabari credentials
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS dpambariuser VARCHAR(255);
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS dpambaripassword VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS dpambariuser;
ALTER TABLE cluster DROP COLUMN IF EXISTS dpambaripassword;
